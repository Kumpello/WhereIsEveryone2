package com.kumpello.whereiseveryone.main.map.presentation

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity.GRANULARITY_FINE
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.MainActivity
import com.kumpello.whereiseveryone.main.common.database.UserLocationDao
import com.kumpello.whereiseveryone.main.common.database.UserLocationEntity
import com.kumpello.whereiseveryone.main.common.domain.usecase.SendLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LocationForegroundService : Service(), LocationServiceProxy.LocationServiceDelegate {
    private val fusedLocationClient: FusedLocationProviderClient by inject()
    private val userLocationDao: UserLocationDao by inject()
    private val sendLocationUseCase: SendLocationUseCase by inject()
    private val locationServiceProxy: LocationServiceProxy by inject()

    private val locationSendChannel = Channel<Location>(
        capacity = Channel.CONFLATED
    )
    private val state = MutableStateFlow(State())
    private val exposedState = state.asStateFlow()

    private val forcedForegroundStatus = MutableStateFlow(LocationService.ForcedForegroundStatus())

    private var lastSendTimestamp: Long = 0L

    private val desiredUpdateType =
        MutableStateFlow<LocationService.UpdateType>(LocationService.UpdateType.Foreground)

    private val binder: IBinder = LocationBinder()
    private val channelID = "WhereIsEveryone_Silent"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val serviceThread =
        HandlerThread("LocationThread", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
        }
    private val handler = Handler(serviceThread.looper)

    override fun onCreate() {
        Timber.tag(TAG).d("LocationForegroundService onCreate")
        super.onCreate()
        locationServiceProxy.registerDelegate(this)
        
        scope.launch {
            getLastLocation()?.let {
                locationServiceProxy.updateLocation(it)
            }
        }
        startLocationSender()

        scope.launch {
            forcedForegroundStatus.collect { status ->
                locationServiceProxy.updateForcedForegroundStatus(status)
            }
        }

        // Periodic notification refresh to update "time since last share"
        scope.launch {
            while (isActive) {
                delay(60.seconds) // Refresh every minute
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("LocationForegroundService onStartCommand: action = %s", intent?.action)

        when (intent?.action) {
            ACTION_STOP_SERVICE, ACTION_KILL_SERVICE -> {
                Timber.tag(TAG).d("Received stop/kill action")
                stopService()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_SHARING -> {
                toggleSharing()
                return START_STICKY
            }
        }

        startServiceInternal()
        return START_STICKY
    }

    private fun startLocationSender() {
        scope.launch {
            while (isActive) {
                val location = locationSendChannel.receive()
                val now = System.currentTimeMillis()
                userLocationDao.updateUserLocation(
                    UserLocationEntity(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        bearing = location.bearing,
                        altitude = location.altitude,
                        accuracy = location.accuracy,
                        lastUpdate = now
                    )
                )
                sendLocation(location, now)
                delay(5_000.milliseconds)
            }
        }
    }

    private fun startServiceInternal() {
        if (!checkPermissions()) {
            Timber.tag(TAG).e("Missing permissions to start LocationForegroundService")
            stopSelf()
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        createNotificationChannel(manager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(420, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(420, createNotification())
        }

        if (!state.value.isLocationUpdatesStarted) {
            startLocationUpdates(updateType = exposedState.value.updateType)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            420, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, LocationForegroundService::class.java).apply {
            action = ACTION_TOGGLE_SHARING
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            422,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val killIntent = Intent(this, LocationForegroundService::class.java).apply {
            action = ACTION_KILL_SERVICE
        }
        val killPendingIntent = PendingIntent.getService(
            this,
            421,
            killIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val isRunning = state.value.isLocationUpdatesStarted
        val statusText = if (isRunning) {
            getString(R.string.notification_status_running)
        } else {
            getString(R.string.notification_status_stopped)
        }

        val lastSharedText = if (lastSendTimestamp == 0L) {
            getString(R.string.last_sent_never)
        } else {
            val minutesAgo = (System.currentTimeMillis() - lastSendTimestamp) / 60_000
            if (minutesAgo < 1) {
                getString(R.string.last_sent_just_now)
            } else {
                getString(R.string.last_sent_format, "$minutesAgo min")
            }
        }

        val toggleActionText = if (isRunning) {
            getString(R.string.stop_sharing)
        } else {
            getString(R.string.resume_sharing)
        }

        val fullText = "$statusText\n$lastSharedText"

        return NotificationCompat.Builder(this, channelID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(fullText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
            .setSubText(getString(R.string.notification_subtext))
            .setSmallIcon(R.drawable.ic_share_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(
                R.drawable.ic_share_location,
                toggleActionText,
                togglePendingIntent
            )
            .addAction(
                R.drawable.ic_share_location,
                getString(R.string.kill_service),
                killPendingIntent
            )
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(420, createNotification())
    }

    private fun checkPermissions(): Boolean {
        val backgroundPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                PackageManager.PERMISSION_GRANTED
            }
        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        return backgroundPermission == PackageManager.PERMISSION_GRANTED ||
                fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("LocationForegroundService stopping")
        locationServiceProxy.unregisterDelegate()
        stopUpdates()
        job.cancel()
        serviceThread.quitSafely()
        super.onDestroy()
    }

    inner class LocationBinder : Binder() {
        val service: LocationForegroundService
            get() = this@LocationForegroundService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val serviceChannel = NotificationChannel(
            channelID,
            "WhereIsEveryone Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(serviceChannel)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                scope.launch {
                    Timber.tag(TAG).d("Emitting location update")
                    locationServiceProxy.updateLocation(location)
                    locationSendChannel.send(location)
                }
            }
        }
    }

    override fun changeUpdateType(updateType: LocationService.UpdateType) {
        desiredUpdateType.value = updateType
        if (forcedForegroundStatus.value.isEnabled) {
            Timber.tag(TAG).d("Change ignored due to forced foreground mode, but saved as desired: %s", updateType)
            return
        }
        applyUpdateType(updateType)
    }

    private fun applyUpdateType(updateType: LocationService.UpdateType) {
        stopUpdates()
        startLocationUpdates(updateType)
    }

    override fun setForcedForeground(durationSeconds: Long?) {
        forcedForegroundStatus.value = LocationService.ForcedForegroundStatus(
            isEnabled = true,
            endTime = durationSeconds?.let { System.currentTimeMillis() + it.seconds.inWholeMilliseconds }
        )
        applyUpdateType(LocationService.UpdateType.Foreground)

        durationSeconds?.let { seconds ->
            scope.launch {
                delay(seconds.seconds)
                if (forcedForegroundStatus.value.isEnabled) {
                    disableForcedForeground()
                }
            }
        }
    }

    override fun disableForcedForeground() {
        forcedForegroundStatus.value = LocationService.ForcedForegroundStatus(isEnabled = false, endTime = null)
        applyUpdateType(desiredUpdateType.value)
    }

    override fun stopService() {
        Timber.tag(TAG).d("stopService called")
        stopUpdates()
        locationServiceProxy.requestUnbind()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun toggleSharing() {
        if (state.value.isLocationUpdatesStarted) {
            Timber.tag(TAG).d("Toggling sharing: Stopping updates")
            stopUpdates()
        } else {
            Timber.tag(TAG).d("Toggling sharing: Starting updates")
            startLocationUpdates(exposedState.value.updateType)
        }
        updateNotification()
    }

    private fun startLocationUpdates(updateType: LocationService.UpdateType) {
        if (state.value.isLocationUpdatesStarted) return

        try {
            fusedLocationClient.requestLocationUpdates(
                when (updateType) {
                    LocationService.UpdateType.Background -> getBackgroundRequest()
                    LocationService.UpdateType.Foreground -> getForegroundRequest()
                },
                locationCallback,
                handler.looper
            )
            state.update { it.copy(isLocationUpdatesStarted = true, updateType = updateType) }
        } catch (exception: SecurityException) {
            SystemClock.sleep(15000)
            Timber.tag(TAG).e("SecurityException during location updates: %s", exception.toString())
        }
    }

    private fun stopUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        state.update { it.copy(isLocationUpdatesStarted = false) }
    }

    private suspend fun sendLocation(location: Location, lastUpdate: Long) {
        runCatching {
            val response = sendLocationUseCase.execute(
                longitude = location.longitude,
                latitude = location.latitude,
                bearing = location.bearing,
                altitude = location.altitude,
                accuracy = location.accuracy,
                lastUpdate = lastUpdate
            )
            if (response is CodeResponse.SuccessNoContent) {
                lastSendTimestamp = System.currentTimeMillis()
                updateNotification()
            } else if (response is CodeResponse.ErrorData) {
                Timber.tag(TAG).e("Error sending location: %s", response.toString())
            }
        }.onFailure { error ->
            Timber.tag(TAG).e("Exception sending location: %s", error.message)
        }
    }

    private fun getForegroundRequest() = LocationRequest.Builder(state.value.foregroundSettings.interval)
        .setGranularity(GRANULARITY_FINE)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setWaitForAccurateLocation(true)
        .setMaxUpdateAgeMillis(state.value.foregroundSettings.maxAge)
        .setMinUpdateIntervalMillis(state.value.foregroundSettings.minInterval)
        .setMaxUpdateDelayMillis(state.value.foregroundSettings.maxDelay)
        .build()

    private fun getBackgroundRequest() = LocationRequest.Builder(state.value.backgroundSettings.interval)
        .setGranularity(GRANULARITY_FINE)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setWaitForAccurateLocation(true)
        .setMaxUpdateAgeMillis(state.value.backgroundSettings.maxAge)
        .setMinUpdateIntervalMillis(state.value.backgroundSettings.minInterval)
        .setMaxUpdateDelayMillis(state.value.backgroundSettings.maxDelay)
        .build()

    override fun changeForegroundUpdateInterval(interval: Long) {
        state.update { it.copy(foregroundSettings = it.foregroundSettings.copy(interval = interval)) }
    }

    override fun changeBackgroundUpdateInterval(interval: Long) {
        state.update { it.copy(backgroundSettings = it.backgroundSettings.copy(interval = interval)) }
    }

    private suspend fun getLastLocation(): Location? {
        return userLocationDao.getUserLocation()?.let { entity ->
            Location("fused").apply {
                latitude = entity.latitude
                longitude = entity.longitude
                bearing = entity.bearing ?: 0f
                altitude = entity.altitude ?: 0.0
                accuracy = entity.accuracy ?: 0f
                time = entity.lastUpdate
            }
        }
    }

    data class State(
        val foregroundSettings: LocationRequestSettings = LocationRequestSettings(
            interval = 5_000L,
            minInterval = 1_000L,
            maxDelay = 30_000L,
            maxAge = 5_000L,
        ),
        val backgroundSettings: LocationRequestSettings = LocationRequestSettings(
            interval = 900_000L,
            minInterval = 300_000L,
            maxDelay = 1_800_000L,
            maxAge = 300_000L
        ),
        val isLocationUpdatesStarted: Boolean = false,
        val updateType: LocationService.UpdateType = LocationService.UpdateType.Foreground
    ) {
        data class LocationRequestSettings(
            val minInterval: Long,
            val maxAge: Long,
            val maxDelay: Long,
            val interval: Long,
        )
    }

    companion object {
        private const val TAG = "LOCATION_SERVICE"
        const val ACTION_STOP_SERVICE = "STOP_LOCATION_SERVICE"
        const val ACTION_TOGGLE_SHARING = "TOGGLE_SHARING"
        const val ACTION_KILL_SERVICE = "KILL_LOCATION_SERVICE"
    }
}
