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
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
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
import com.kumpello.whereiseveryone.main.MainActivity
import com.kumpello.whereiseveryone.main.map.domain.usecase.SendLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class LocationServiceImpl(
    //private val fusedLocationClient: FusedLocationProviderClient,
    //private val fusedOrientationClient: FusedOrientationProviderClient
) : Service(), LocationService {
    private val fusedLocationClient: FusedLocationProviderClient by inject()

    private val state = MutableStateFlow(State())
    private val exposedState = state.asStateFlow() //TODO: Change "exposed" to something else
    private val locationFlow = MutableSharedFlow<Location>()
    private val exposedLocationFlow = locationFlow.asSharedFlow()

    private val binder: IBinder = LocationBinder()
    private val channelID = "WhereIsEveryone"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val sendLocationUseCase: SendLocationUseCase by inject()

    override fun onCreate() {
        Timber.d("LocationService onCreate")
        super.onCreate()

        startService()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.d("LocationService onStartCommand")

        startService()
        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    private fun startService() {
        Timber.d("LocationService checking permissions")
        checkPermissions()
        Timber.d("LocationService starting")

        //val input = intent.getStringExtra(STATUS_PARAM)
        val manager = getSystemService(NotificationManager::class.java)
        createNotificationChannel(manager)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            420, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        //TODO: Add icons and logo
        val notification: Notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getString(R.string.click_here_to_go_to_map))
            //.setSubText(getString(R.string.click_here_to_go_to_map))
            .setSmallIcon(R.drawable.ic_share_location)
            //.setTicker(getText(R.string.ticker_text))
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(420, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(420, notification)
        }

        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
            startLocationUpdates(updateType = exposedState.value.updateType)
        }
    }

    private fun checkPermissions() {
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

        if (
            backgroundPermission == PackageManager.PERMISSION_DENIED
            || fineLocationPermission == PackageManager.PERMISSION_DENIED
            || coarseLocationPermission == PackageManager.PERMISSION_DENIED
        ) {
            // TODO: Consider informing user or updating your app UI if visible.
            stopSelf()
            return
        }
    }

    override fun onDestroy() {
        Timber.d("LocationService stopping")
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
            for (location in locationResult.locations) {
                scope.launch {
                    locationFlow.emit(location)
                    Timber.d("Emiting location")
                    sendLocation(location)
                }
            }
        }
    }

    override fun changeUpdateType(updateType: LocationService.UpdateType) {
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

    private fun startLocationUpdates(updateType: LocationService.UpdateType) {
        if (state.value.updateLocation) {
            try {
                Timber.d("Trying to send location")
                fusedLocationClient.requestLocationUpdates(
                    when (updateType) {
                        LocationService.UpdateType.Background -> getBackgroundRequest()
                        LocationService.UpdateType.Foreground -> getForegroundRequest()
                    },
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (exception: SecurityException) {
                SystemClock.sleep(15000)
                Timber.e(exception.toString())
            }
        }
    }

    private fun stopUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        state.update {
            it.copy(
                updateLocation = false
            )
        }
    }

    private fun sendLocation(location: Location) {
        scope.launch {
            runCatching {
                sendLocationUseCase.execute(location.longitude, location.latitude) //TODO: Add parameters!
                    .let { response ->
                        Timber.d("Sending location code $response") //TODO: Send to UI if failed
                    }
            }.onFailure { error ->
                Timber.d(error) //TODO: Send to UI if error
            }
        }
    }

    private fun getForegroundRequest() =
        LocationRequest //TODO: Get settings directly LocationRequestSettings as extension, or soome other better way
            .Builder(state.value.foregroundSettings.interval)
            .setGranularity(GRANULARITY_FINE)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateAgeMillis(state.value.foregroundSettings.maxAge)
            .setMinUpdateIntervalMillis(state.value.foregroundSettings.minInterval)
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
        .build().also {
            state.update {
                it.copy(
                    updateType = LocationService.UpdateType.Background
                )
            }
        }

    override fun changeForegroundUpdateInterval(interval: Long) {
        state.update {
            it.copy(
                foregroundSettings = state.value.foregroundSettings.copy(
                    interval = interval
                )
            )
        }
    }

    override fun changeBackgroundUpdateInterval(interval: Long) {
        state.update {
            it.copy(
                foregroundSettings = state.value.foregroundSettings.copy(
                    interval = interval
                )
            )
        }
    }

    override fun getLocation(): Flow<Location> {
        return exposedLocationFlow
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
        val updateLocation: Boolean = true,
        val updateType: LocationService.UpdateType = LocationService.UpdateType.Foreground
    ) {
        data class LocationRequestSettings(
            val minInterval: Long,
            val maxAge: Long,
            val maxDelay: Long,
            val interval: Long,
        )
    }
}