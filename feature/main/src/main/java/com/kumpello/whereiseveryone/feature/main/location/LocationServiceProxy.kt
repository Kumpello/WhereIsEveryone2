package com.kumpello.whereiseveryone.feature.main.location

import com.kumpello.whereiseveryone.main.map.presentation.LocationForegroundService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.app.PendingIntent
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import com.kumpello.whereiseveryone.data.model.LocationData
import com.mapbox.common.Cancelable
import com.mapbox.common.location.DeviceLocationProvider
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.LocationObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import timber.log.Timber

/** One acquisition source for the app and Mapbox's standard puck provider. */
class LocationServiceProxy(private val context: Context) : LocationService, DeviceLocationProvider {

    @Volatile
    private var delegate: LocationServiceDelegate? = null

    private val _locationFlow = MutableStateFlow<Location?>(null)
    private val _forcedForegroundStatus = MutableStateFlow(LocationService.ForcedForegroundStatus())
    private val _isServiceRunning = MutableStateFlow(false)
    private val _status = MutableStateFlow(LocationService.Status())
    private val _mapDisplayLocation = MutableStateFlow<LocationData?>(null)
    private val mapScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mapObservers = ConcurrentHashMap<LocationObserver, Job>()
    private val pendingObservers = ConcurrentHashMap<PendingIntent, LocationObserver>()

    private var stopRequested = false
    private var appForeground = true

    fun registerDelegate(delegate: LocationServiceDelegate) {
        Timber.tag(TAG).d("Registering service delegate")
        this.delegate = delegate
        if (stopRequested) {
            delegate.stopService()
        } else {
            delegate.setAppForeground(appForeground)
            _isServiceRunning.value = true
        }
    }

    fun unregisterDelegate() {
        Timber.tag(TAG).d("Unregistering service delegate")
        this.delegate = null
        _mapDisplayLocation.value = null
        _isServiceRunning.value = false
        try {
            context.unbindService(connection)
        } catch (e: IllegalArgumentException) {
            Timber.tag(TAG).d(e, "Service already unbound during unregister")
        }
    }

    fun updateLocation(location: Location?) {
        _locationFlow.value = location
    }

    fun updateForcedForegroundStatus(status: LocationService.ForcedForegroundStatus) {
        _forcedForegroundStatus.value = status
    }

    fun updateStatus(status: LocationService.Status) {
        _status.value = status
        if (!status.isCarMode || !status.isAppForeground) _mapDisplayLocation.value = null
    }

    override fun startLocationService() {
        stopRequested = false
        Timber.tag(TAG).d("Starting foreground location service")
        val intent = Intent(context, LocationForegroundService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun stopLocationService() {
        stopRequested = true
        _isServiceRunning.value = false
        _mapDisplayLocation.value = null
        Timber.tag(TAG).d("Stopping foreground location service")
        delegate?.stopService()
        try {
            context.unbindService(connection)
        } catch (e: IllegalArgumentException) {
            Timber.tag(TAG).d(e, "Service already unbound during stop")
        }
        val intent = Intent(context, LocationForegroundService::class.java)
        context.stopService(intent)
    }

    override fun toggleSharing() {
        delegate?.toggleSharing()
    }

    override fun observeIsServiceRunning(): StateFlow<Boolean> {
        return _isServiceRunning.asStateFlow()
    }

    override fun changeForegroundUpdateInterval(interval: Long) {
        delegate?.changeForegroundUpdateInterval(interval)
    }

    override fun changeBackgroundUpdateInterval(interval: Long) {
        delegate?.changeBackgroundUpdateInterval(interval)
    }

    override fun observeLocation(): StateFlow<Location?> {
        return _locationFlow.asStateFlow()
    }

    override fun setAppForeground(isForeground: Boolean) {
        appForeground = isForeground
        delegate?.setAppForeground(isForeground)
    }

    override fun observeStatus(): StateFlow<LocationService.Status> = _status.asStateFlow()

    override fun setMapDisplayLocation(location: LocationData?) {
        _mapDisplayLocation.value = location
    }

    override fun getName(): String = "whereiseveryone-location-service"

    override fun addLocationObserver(observer: LocationObserver) = addLocationObserver(observer, Looper.getMainLooper())

    override fun addLocationObserver(observer: LocationObserver, looper: Looper) {
        mapObservers.remove(observer)?.cancel()
        val dispatcher = Handler(looper).asCoroutineDispatcher().immediate
        val job = mapScope.launch(dispatcher, start = CoroutineStart.LAZY) {
            combine(_locationFlow, _mapDisplayLocation, _status) { raw, display, status ->
                mapDisplayFix(raw?.asLocationData(), display, status, System.currentTimeMillis())
            }.distinctUntilChanged().collect { fix ->
                if (fix != null) {
                    val location = fix.asMapboxLocation()
                    observer.onLocationUpdateReceived(listOf(location))
                }
            }
        }
        mapObservers[observer] = job
        job.start()
    }

    override fun removeLocationObserver(observer: LocationObserver) {
        mapObservers.remove(observer)?.cancel()
    }

    override fun getLastLocation(callback: GetLocationCallback): Cancelable {
        val fix = mapDisplayFix(_locationFlow.value?.asLocationData(), _mapDisplayLocation.value, _status.value, System.currentTimeMillis())
        callback.run(fix?.asMapboxLocation())
        return Cancelable { }
    }

    override fun requestLocationUpdates(pendingIntent: PendingIntent) {
        removeLocationUpdates(pendingIntent)
        var lastTime = Long.MIN_VALUE
        val observer = LocationObserver {
            val fix = _locationFlow.value ?: return@LocationObserver
            if (fix.time == lastTime) return@LocationObserver
            lastTime = fix.time
            try {
                pendingIntent.send(context, 0, Intent().putExtra(LocationManager.KEY_LOCATION_CHANGED, Location(fix)))
            } catch (_: PendingIntent.CanceledException) {
                removeLocationUpdates(pendingIntent)
            }
        }
        pendingObservers[pendingIntent] = observer
        addLocationObserver(observer)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent) {
        pendingObservers.remove(pendingIntent)?.let(::removeLocationObserver)
    }

    override fun setForcedForeground(durationSeconds: Long?) {
        delegate?.setForcedForeground(durationSeconds)
    }

    override fun disableForcedForeground() {
        delegate?.disableForcedForeground()
    }

    override fun observeForcedForegroundStatus(): StateFlow<LocationService.ForcedForegroundStatus> {
        return _forcedForegroundStatus.asStateFlow()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.tag(TAG).d("Service bound")
            _isServiceRunning.value = delegate != null && !stopRequested
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.tag(TAG).d("Service unbound")
            _isServiceRunning.value = false
        }
    }

    interface LocationServiceDelegate {
        fun changeForegroundUpdateInterval(interval: Long)
        fun changeBackgroundUpdateInterval(interval: Long)
        fun setAppForeground(isForeground: Boolean)
        fun setForcedForeground(durationSeconds: Long?)
        fun disableForcedForeground()
        fun stopService()
        fun toggleSharing()
    }

    companion object {
        private const val TAG = "LOCATION_PROXY"
    }
}

private fun Location.asLocationData() = LocationData(
    lat = latitude, lon = longitude,
    bearing = bearing.takeIf { hasBearing() },
    alt = altitude.takeIf { hasAltitude() },
    accuracy = accuracy.takeIf { hasAccuracy() },
    speed = speed.takeIf { hasSpeed() },
    lastUpdate = time,
)

private fun LocationData.asMapboxLocation(): com.mapbox.common.location.Location =
    com.mapbox.common.location.Location.Builder()
        .latitude(lat).longitude(lon).timestamp(lastUpdate)
        .bearing(bearing?.toDouble()?.takeIf { it.isFinite() })
        .altitude(alt).horizontalAccuracy(accuracy?.toDouble())
        .speed(speed?.toDouble()).source("whereiseveryone")
        .build()
