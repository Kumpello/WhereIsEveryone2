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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class LocationServiceImpl : Service(), LocationService {
    private val fusedLocationClient: FusedLocationProviderClient by inject()
    private val userLocationDao: UserLocationDao by inject()
    private val sendLocationUseCase: SendLocationUseCase by inject()

    private val locationSendChannel = Channel<Location>(
        capacity = Channel.CONFLATED
    )
    private val state = MutableStateFlow(State())
    private val exposedState = state.asStateFlow()
    private val locationFlow = MutableStateFlow<Location?>(null)
    private val exposedLocationFlow = locationFlow.asStateFlow()

    private val isForcedForeground = MutableStateFlow(false)
    private val exposedIsForcedForeground = isForcedForeground.asStateFlow()

    private val desiredUpdateType = MutableStateFlow<LocationService.UpdateType>(LocationService.UpdateType.Foreground)

    private val binder: IBinder = LocationBinder()
    private val channelID = "WhereIsEveryone"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val serviceThread = HandlerThread("LocationThread", Process.THREAD_PRIORITY_BACKGROUND).apply {
        start()
    }
    private val handler = Handler(serviceThread.looper)

    override fun onCreate() {
        Timber.tag(TAG).d("LocationService onCreate")
        super.onCreate()
        LocationServiceImpl.state.value = true
        scope.launch {
            getLastLocation()?.let { locationFlow.emit(it) }
        }
        startLocationSender()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("LocationService onStartCommand")

        startService()
        return START_STICKY
    }

    private fun startLocationSender() {
        scope.launch {
            while (isActive) {
                // Wait for a new location update
                val location = locationSendChannel.receive()

                // Process and send the location
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
                sendLocation(
                    location,
                    now
                )

                // Enforce at least 5 seconds delay before the next potential send
                delay(5_000.milliseconds)
            }
        }
    }

    private fun startService() {
        Timber.tag(TAG).d("LocationService checking permissions")
        if (!checkPermissions()) {
            Timber.tag(TAG).e("Missing permissions to start LocationService")
            stopSelf()
            return
        }
        
        if (state.value.isLocationUpdatesStarted) {
            Timber.tag(TAG).d("LocationService already running")
            return
        }
        
        Timber.tag(TAG).d("LocationService starting")

        val manager = getSystemService(NotificationManager::class.java)
        createNotificationChannel(manager)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            420, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getString(R.string.click_here_to_go_to_map))
            .setSubText(getString(R.string.notification_subtext))
            .setSmallIcon(R.drawable.ic_share_location)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(420, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(420, notification)
        }

        startLocationUpdates(updateType = exposedState.value.updateType)
    }

    private fun checkPermissions(): Boolean {
        val backgroundPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                PackageManager.PERMISSION_GRANTED
            }
        val fineLocationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        return backgroundPermission == PackageManager.PERMISSION_GRANTED
                || fineLocationPermission == PackageManager.PERMISSION_GRANTED
                || coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("LocationService stopping")
        stopUpdates()
        LocationServiceImpl.state.value = false
        job.cancel()
        serviceThread.quitSafely()
        super.onDestroy()
    }

    inner class LocationBinder : Binder() {
        val service: LocationServiceImpl
            get() = this@LocationServiceImpl
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val serviceChannel = NotificationChannel(
            channelID,
            "WhereIsEveryone Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(serviceChannel)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                scope.launch {
                    Timber.tag(TAG).d("Emitting location update")
                    locationFlow.emit(location)
                    locationSendChannel.send(location)
                }
            }
        }
    }

    override fun changeUpdateType(updateType: LocationService.UpdateType) {
        desiredUpdateType.value = updateType
        if (isForcedForeground.value) {
            Timber.tag(TAG).d("Change ignored due to forced foreground mode, but saved as desired: %s", updateType)
            return
        }
        Timber.tag(TAG).d("Changing update type to: %s", updateType)
        applyUpdateType(updateType)
    }

    private fun applyUpdateType(updateType: LocationService.UpdateType) {
        when (updateType) {
            LocationService.UpdateType.Background -> {
                stopUpdates()
                startLocationUpdates(updateType)
            }

            LocationService.UpdateType.Foreground -> {
                stopUpdates()
                startLocationUpdates(updateType)
            }
        }
    }

    override fun setForcedForeground(durationMinutes: Int?) {
        Timber.tag(TAG).d("Setting forced foreground for %s minutes", durationMinutes)
        isForcedForeground.value = true
        stopUpdates()
        applyUpdateType(LocationService.UpdateType.Foreground)

        durationMinutes?.let { minutes ->
            scope.launch {
                delay(minutes.minutes)
                if (isForcedForeground.value) {
                    Timber.tag(TAG).d("Forced foreground duration expired")
                    disableForcedForeground()
                }
            }
        }
    }

    override fun disableForcedForeground() {
        Timber.tag(TAG).d("Disabling forced foreground")
        isForcedForeground.value = false
        applyUpdateType(desiredUpdateType.value)
    }

    override fun observeForcedForeground(): StateFlow<Boolean> {
        return exposedIsForcedForeground
    }

    private fun startLocationUpdates(updateType: LocationService.UpdateType) {
        if (state.value.isLocationUpdatesStarted) return

        try {
            Timber.tag(TAG).d("Starting location updates, type: %s", updateType)
            fusedLocationClient.requestLocationUpdates(
                when (updateType) {
                    LocationService.UpdateType.Background -> getBackgroundRequest()
                    LocationService.UpdateType.Foreground -> getForegroundRequest()
                },
                locationCallback,
                handler.looper
            )
            state.update {
                it.copy(
                    isLocationUpdatesStarted = true
                )
            }
        } catch (exception: SecurityException) {
            SystemClock.sleep(15000)
            Timber.tag(TAG).e("SecurityException during location updates: %s", exception.toString())
        }
    }

    private fun stopUpdates() {
        Timber.tag(TAG).d("Stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        state.update {
            it.copy(
                isLocationUpdatesStarted = false
            )
        }
    }

    private suspend fun sendLocation(location: Location, lastUpdate: Long) {
        runCatching {
            Timber.tag(TAG).d("Sending location to backend")
            val response = sendLocationUseCase.execute(
                longitude = location.longitude,
                latitude = location.latitude,
                bearing = location.bearing,
                altitude = location.altitude,
                accuracy = location.accuracy,
                lastUpdate = lastUpdate
            )
            when (response) {
                is CodeResponse.ErrorData -> {
                    Timber.tag(TAG).e("Error sending location: %s", response.toString())
                }
                CodeResponse.SuccessNoContent -> {
                    Timber.tag(TAG).d("Location sent successfully")
                }
            }
        }.onFailure { error ->
            Timber.tag(TAG).e("Exception sending location: %s", error.message)
        }
    }

    private fun getForegroundRequest() =
        LocationRequest
            .Builder(state.value.foregroundSettings.interval)
            .setGranularity(GRANULARITY_FINE)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateAgeMillis(state.value.foregroundSettings.maxAge)
            .setMinUpdateIntervalMillis(state.value.foregroundSettings.minInterval)
            .setMaxUpdateDelayMillis(state.value.foregroundSettings.maxDelay)
            .build().also {
                state.update {
                    it.copy(
                        updateType = LocationService.UpdateType.Foreground
                    )
                }
            }

    private fun getBackgroundRequest() = LocationRequest
        .Builder(state.value.backgroundSettings.interval)
        .setGranularity(GRANULARITY_FINE)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setWaitForAccurateLocation(true)
        .setMaxUpdateAgeMillis(state.value.backgroundSettings.maxAge)
        .setMinUpdateIntervalMillis(state.value.backgroundSettings.minInterval)
        .setMaxUpdateDelayMillis(state.value.backgroundSettings.maxDelay)
        .build().also {
            state.update {
                it.copy(
                    updateType = LocationService.UpdateType.Background
                )
            }
        }

    override fun changeForegroundUpdateInterval(interval: Long) {
        Timber.tag(TAG).d("Changing foreground interval to: %s", interval)
        state.update {
            it.copy(
                foregroundSettings = state.value.foregroundSettings.copy(
                    interval = interval
                )
            )
        }
    }

    override fun changeBackgroundUpdateInterval(interval: Long) {
        Timber.tag(TAG).d("Changing background interval to: %s", interval)
        state.update {
            it.copy(
                backgroundSettings = state.value.backgroundSettings.copy(
                    interval = interval
                )
            )
        }
    }

    override fun stopLocationService() {
        Timber.tag(TAG).d("Stopping location service manually")
        stopUpdates()
        stopSelf()
    }

    override fun observeLocation(): StateFlow<Location?> {
        return exposedLocationFlow
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
            interval = 5_000L, // 5s
            minInterval = 1_000L, // 1s
            maxDelay = 30_000L, //30s
            maxAge = 5_000L, // 5s
        ),
        val backgroundSettings: LocationRequestSettings = LocationRequestSettings(
            interval = 900_000L, // 15min
            minInterval = 300_000L, // 5min
            maxDelay = 1_800_000L, // 30min
            maxAge = 300_000L // 5min
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
        private val state: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val stateFlow: StateFlow<Boolean> = state.asStateFlow()
        private const val TAG = "LOCATION_SERVICE"
    }

}
