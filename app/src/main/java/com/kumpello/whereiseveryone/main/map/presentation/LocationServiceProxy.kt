package com.kumpello.whereiseveryone.main.map.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class LocationServiceProxy : LocationService, KoinComponent {
    private val context: Context by inject()

    @Volatile
    private var delegate: LocationServiceDelegate? = null

    private val _locationFlow = MutableStateFlow<Location?>(null)
    private val _forcedForegroundStatus = MutableStateFlow(LocationService.ForcedForegroundStatus())
    private val _isServiceRunning = MutableStateFlow(false)

    fun registerDelegate(delegate: LocationServiceDelegate) {
        Timber.tag(TAG).d("Registering service delegate")
        this.delegate = delegate
        _isServiceRunning.value = true
    }

    fun unregisterDelegate() {
        Timber.tag(TAG).d("Unregistering service delegate")
        this.delegate = null
        _isServiceRunning.value = false
        try {
            context.unbindService(connection)
        } catch (e: IllegalArgumentException) {
            Timber.tag(TAG).e("Service not bound during unregister: %s", e.message)
        }
    }

    fun updateLocation(location: Location?) {
        _locationFlow.value = location
    }

    fun updateForcedForegroundStatus(status: LocationService.ForcedForegroundStatus) {
        _forcedForegroundStatus.value = status
    }

    override fun startLocationService() {
        Timber.tag(TAG).d("Starting foreground location service")
        val intent = Intent(context, LocationForegroundService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun stopLocationService() {
        Timber.tag(TAG).d("Stopping foreground location service")
        delegate?.stopService()
        try {
            context.unbindService(connection)
        } catch (e: IllegalArgumentException) {
            Timber.tag(TAG).e("Service not bound: %s", e.message)
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

    override fun changeUpdateType(updateType: LocationService.UpdateType) {
        delegate?.changeUpdateType(updateType)
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
            _isServiceRunning.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.tag(TAG).d("Service unbound")
            _isServiceRunning.value = false
        }
    }

    interface LocationServiceDelegate {
        fun changeForegroundUpdateInterval(interval: Long)
        fun changeBackgroundUpdateInterval(interval: Long)
        fun changeUpdateType(updateType: LocationService.UpdateType)
        fun setForcedForeground(durationSeconds: Long?)
        fun disableForcedForeground()
        fun stopService()
        fun toggleSharing()
    }

    companion object {
        private const val TAG = "LOCATION_PROXY"
    }
}
