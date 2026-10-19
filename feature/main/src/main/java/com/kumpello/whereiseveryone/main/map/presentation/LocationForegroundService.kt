package com.kumpello.whereiseveryone.main.map.presentation

import com.kumpello.whereiseveryone.feature.main.location.LocationService
import com.kumpello.whereiseveryone.feature.main.location.LocationServiceProxy
import com.kumpello.whereiseveryone.feature.main.location.LocationUpload
import com.kumpello.whereiseveryone.feature.main.location.sendLocationWithRetry
import com.kumpello.whereiseveryone.feature.main.location.chargingStatus
import com.kumpello.whereiseveryone.feature.main.location.resolveLocationUpdateType
import com.kumpello.whereiseveryone.feature.main.location.shouldAcquireLocations
import com.kumpello.whereiseveryone.feature.main.location.uploadIntervalMillis
import com.kumpello.whereiseveryone.feature.main.R
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
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity.GRANULARITY_FINE
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.feature.main.navigation.MainActivityIntentFactory
import com.kumpello.whereiseveryone.feature.main.notifications.FriendRequestNotifications
import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.repository.UserLocationRepository
import com.kumpello.whereiseveryone.feature.main.domain.manager.ProximityManager
import com.kumpello.whereiseveryone.feature.main.domain.usecase.SendLocationUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LocationForegroundService : Service(), LocationServiceProxy.LocationServiceDelegate {
    private val mainActivityIntentFactory: MainActivityIntentFactory by inject()
    private val fusedLocationClient: FusedLocationProviderClient by inject()
    private val userLocationRepository: UserLocationRepository by inject()
    private val sendLocationUseCase: SendLocationUseCase by inject()
    private val locationServiceProxy: LocationServiceProxy by inject()
    private val proximityManager: ProximityManager by inject()
    private val preferencesManager: PreferencesManager by inject()
    private val friendRequestNotifications: FriendRequestNotifications by inject()

    private val locationSendChannel = Channel<LocationUpload>(
        capacity = Channel.CONFLATED
    )
    private val state = MutableStateFlow(State())

    private var activeRequest: LocationRequest? = null

    private val binder: IBinder = LocationBinder()

    private val channelID = "WhereIsEveryone_Silent"
    private val proximityChannelID = "WhereIsEveryone_Proximity"
    private val proximityNotificationID = 422
    private val job = SupervisorJob()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + job)
    private var locationUpdatesJob: Job? = null
    private var locationSenderJob: Job? = null
    private var forcedForegroundTimeoutJob: Job? = null
    private val sharingMutex = Mutex()
    private var isServiceStarted = false
    private var previousNearbyFriends = emptySet<String>()

    private var latestLocation: Location? = null

    private var lastSendTimestamp: Long = 0L

    override fun onCreate() {
        Timber.tag(TAG).d("LocationForegroundService onCreate")
        super.onCreate()
        locationServiceProxy.registerDelegate(this)

        scope.launch { friendRequestNotifications.observe() }

        scope.launch {
            chargingStatus().collect { charging ->
                state.update { it.copy(isCharging = charging) }
                evaluateLocationState()
            }
        }
        scope.launch {
            preferencesManager.observe(PreferencesKey.CarMode)
                .map { it == true }
                .distinctUntilChanged()
                .collect { enabled ->
                    state.update { it.copy(isCarMode = enabled) }
                    evaluateLocationState()
                }
        }

        scope.launch {
            preferencesManager.observe(PreferencesKey.LocationSharingEnabled)
                .map { it != false }
                .distinctUntilChanged()
                .catch { exception ->
                    Timber.tag(TAG).e(exception, "Unable to observe location sharing preference")
                    stopService()
                }
                .collect { enabled ->
                    state.update { it.copy(isSharingEnabled = enabled) }
                    evaluateLocationState()
                }
        }

        scope.launch {
            getLastLocation()?.let {
                if (latestLocation == null) locationServiceProxy.updateLocation(it)
            }
        }

        scope.launch {
            proximityManager.observeNearbyFriends().collect { nearbyFriends ->
                updateProximityNotification(nearbyFriends)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("LocationForegroundService onStartCommand: action = %s", intent?.action)

        when (intent?.action) {
            ACTION_KILL_SERVICE -> {
                Timber.tag(TAG).d("Received stop/kill action")
                locationServiceProxy.stopLocationService()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_SHARING -> {
                startServiceInternal(startUpdates = false)
                toggleSharing()
                return START_STICKY
            }
        }

        startServiceInternal()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.tag(TAG).d("App removed from Recent Apps, stopping location service")
        locationServiceProxy.stopLocationService()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("LocationForegroundService stopping")
        isServiceStarted = false
        locationServiceProxy.unregisterDelegate()
        stopUpdates()
        locationSendChannel.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun setAppForeground(isForeground: Boolean) {
        state.update { it.copy(isAppForeground = isForeground) }
        evaluateLocationState()
    }

    override fun setForcedForeground(durationSeconds: Long?) {
        forcedForegroundTimeoutJob?.cancel()
        state.update { it.copy(forcedForegroundStatus = LocationService.ForcedForegroundStatus(
            isEnabled = true,
            endTime = durationSeconds?.let { System.currentTimeMillis() + it.seconds.inWholeMilliseconds }
        )) }
        evaluateLocationState()

        durationSeconds?.let { seconds ->
            forcedForegroundTimeoutJob = scope.launch {
                delay(seconds.seconds)
                if (state.value.forcedForegroundStatus.isEnabled) {
                    disableForcedForeground()
                }
            }
        }
    }

    override fun disableForcedForeground() {
        forcedForegroundTimeoutJob?.cancel()
        forcedForegroundTimeoutJob = null
        state.update { it.copy(forcedForegroundStatus = LocationService.ForcedForegroundStatus()) }
        evaluateLocationState()
    }

    override fun stopService() {
        Timber.tag(TAG).d("stopService called")
        isServiceStarted = false
        stopUpdates()
        locationSendChannel.cancel()
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun toggleSharing() {
        scope.launch {
            try {
                sharingMutex.withLock {
                    val enabled = preferencesManager.get(PreferencesKey.LocationSharingEnabled) != false
                    preferencesManager.save(PreferencesKey.LocationSharingEnabled, !enabled)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.tag(TAG).e(exception, "Unable to save location sharing preference")
            }
        }
    }

    override fun changeForegroundUpdateInterval(interval: Long) {
        state.update { it.copy(foregroundSettings = it.foregroundSettings.copy(interval = interval)) }
        evaluateLocationState()
    }

    override fun changeBackgroundUpdateInterval(interval: Long) {
        state.update { it.copy(backgroundSettings = it.backgroundSettings.copy(interval = interval)) }
        evaluateLocationState()
    }

    private fun updateProximityNotification(nearbyFriends: List<String>) {
        val currentSet = nearbyFriends.toSet()
        val manager = getSystemService(NotificationManager::class.java)
        
        if (currentSet.isEmpty()) {
            if (previousNearbyFriends.isNotEmpty()) {
                manager.cancel(proximityNotificationID)
                previousNearbyFriends = emptySet()
            }
            return
        }

        val joined = currentSet - previousNearbyFriends
        val left = previousNearbyFriends - currentSet
        
        val lastChange = when {
            joined.isNotEmpty() -> getString(R.string.proximity_alert_joined, joined.joinToString(", "))
            left.isNotEmpty() -> getString(R.string.proximity_alert_left, left.joinToString(", "))
            else -> null
        }

        // Only update previousNearbyFriends if there was a change to avoid losing lastChange context on same-set updates
        if (lastChange != null) {
            previousNearbyFriends = currentSet
        }

        createProximityNotificationChannel(manager)

        val contentText = getString(R.string.proximity_alert_content, nearbyFriends.joinToString(", "))
        val fullText = if (lastChange != null) "$lastChange\n\n$contentText" else contentText

        val notificationIntent = mainActivityIntentFactory.create(this)
        val pendingIntent = PendingIntent.getActivity(
            this,
            proximityNotificationID,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, proximityChannelID)
            .setContentTitle(getString(R.string.proximity_alert_title))
            .setContentText(lastChange ?: contentText)
            .setSmallIcon(R.drawable.ic_share_location)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false) // Allow alerting on each friend entering/leaving
            .build()

        manager.notify(proximityNotificationID, notification)
    }

    private fun createProximityNotificationChannel(notificationManager: NotificationManager) {
        if (notificationManager.getNotificationChannel(proximityChannelID) == null) {
            val serviceChannel = NotificationChannel(
                proximityChannelID,
                getString(R.string.proximity_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startLocationSender() {
        locationSenderJob = scope.launch {
            while (isActive) {
                sendLocation(locationSendChannel.receive())
                // Keep GPS acquisition independent of network throughput. Conflation retains
                // the newest fix while a single upload is in flight, including on fast GNSS devices.
                delay(state.value.updateType.uploadIntervalMillis().milliseconds)
            }
        }
    }

    private fun startServiceInternal(startUpdates: Boolean = true) {
        if (!scope.isActive) return
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

        isServiceStarted = true
        if (startUpdates) {
            evaluateLocationState()
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = mainActivityIntentFactory.create(this)
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

        val toggleActionText = if (state.value.isSharingEnabled) {
            getString(R.string.stop_sharing)
        } else {
            getString(R.string.resume_sharing)
        }

        val fullText = "$statusText\n$lastSharedText"

        return NotificationCompat.Builder(this, channelID)
            .setContentTitle(getString(R.string.location_sharing))
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
        if (!isServiceStarted) return
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

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val serviceChannel = NotificationChannel(
            channelID,
            "WhereIsEveryone Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(serviceChannel)
    }

    /** Every input changes the same state; only a changed request restarts GPS acquisition. */
    private fun evaluateLocationState() {
        val snapshot = state.value
        val mode = resolveLocationUpdateType(
            appForeground = snapshot.isAppForeground,
            forcedForeground = snapshot.forcedForegroundStatus.isEnabled,
            isCharging = snapshot.isCharging,
            isCarMode = snapshot.isCarMode,
        )
        state.update { it.copy(updateType = mode) }
        locationServiceProxy.updateStatus(LocationService.Status(
            updateType = mode,
            isAppForeground = snapshot.isAppForeground,
            isCarMode = snapshot.isCarMode,
            isCharging = snapshot.isCharging,
            isSharingEnabled = snapshot.isSharingEnabled,
        ))
        locationServiceProxy.updateForcedForegroundStatus(snapshot.forcedForegroundStatus)
        val request = if (isServiceStarted && scope.isActive && shouldAcquireLocations(snapshot.isSharingEnabled, snapshot.isAppForeground)) {
            when (mode) {
                LocationService.UpdateType.Charging -> getChargingRequest()
                LocationService.UpdateType.CarMode -> getCarModeRequest()
                LocationService.UpdateType.Foreground -> getForegroundRequest()
                LocationService.UpdateType.Background -> getBackgroundRequest()
            }
        } else null
        if (request != activeRequest || (request != null && locationUpdatesJob?.isActive != true)) {
            stopUpdates()
            if (request != null) startLocationUpdates(request)
        }
        if (snapshot.isSharingEnabled && locationUpdatesJob?.isActive == true) {
            if (locationSenderJob?.isActive != true) startLocationSender()
        } else stopLocationSender()
        updateNotification()
    }

    private fun startLocationUpdates(request: LocationRequest) {
        activeRequest = request
        state.update { it.copy(isLocationUpdatesStarted = true) }
        updateNotification()
        locationUpdatesJob = scope.launch(Dispatchers.Main) {
            try {
                fusedLocationClient.locationUpdates(request, Looper.getMainLooper())
                    .catch { exception ->
                        Timber.tag(TAG).e(exception, "Unable to receive location updates")
                    }
                    .collect { location ->
                        if (location.time <= (latestLocation?.time ?: 0L)) return@collect
                        Timber.tag(TAG).d("Emitting location update")
                        latestLocation = location
                        locationServiceProxy.updateLocation(location)
                        if (state.value.isSharingEnabled) {
                            locationSendChannel.send(LocationUpload(location, isCarMode = state.value.isCarMode))
                        }
                    }
            } finally {
                // A cancelled collection must not reset a replacement collection's state.
                if (locationUpdatesJob === coroutineContext[Job]) {
                    stopUpdates()
                    updateNotification()
                }
            }
        }
    }

    private fun stopUpdates() {
        activeRequest = null
        val updates = locationUpdatesJob
        locationUpdatesJob = null
        updates?.cancel()
        stopLocationSender()
        state.update { it.copy(isLocationUpdatesStarted = false) }
    }

    private fun stopLocationSender() {
        locationSenderJob?.cancel()
        locationSenderJob = null
        locationSendChannel.tryReceive()
    }

    private suspend fun sendLocation(initialUpload: LocationUpload) {
        try {
            val fix = initialUpload.location
            // Persist paced fixes with suspend Room calls instead of blocking service teardown.
            userLocationRepository.saveLocation(LocationData(
                lat = fix.latitude,
                lon = fix.longitude,
                bearing = fix.bearing.takeIf { fix.hasBearing() },
                alt = fix.altitude.takeIf { fix.hasAltitude() },
                accuracy = fix.accuracy.takeIf { fix.hasAccuracy() },
                speed = fix.speed.takeIf { fix.hasSpeed() },
                lastUpdate = initialUpload.lastUpdate,
                isCarMode = initialUpload.isCarMode,
            ))
            val response = sendLocationWithRetry(
                initialUpload = initialUpload,
                takeLatestUpload = { locationSendChannel.tryReceive().getOrNull() }
            ) { upload ->
                val location = upload.location
                sendLocationUseCase.execute(
                    longitude = location.longitude,
                    latitude = location.latitude,
                    bearing = location.bearing.takeIf { location.hasBearing() },
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    speed = location.speed.takeIf { location.hasSpeed() },
                    lastUpdate = upload.lastUpdate,
                    isCarMode = upload.isCarMode,
                )
            }
            if (response is CodeResponse.SuccessNoContent) {
                lastSendTimestamp = System.currentTimeMillis()
                updateNotification()
            } else if (response is CodeResponse.ErrorData) {
                Timber.tag(TAG).d("Location update rejected")
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (error: Exception) {
            Timber.tag(TAG).w(error, "Unable to send location update")
        }
    }

    private fun getChargingRequest() = LocationRequest.Builder(0L)
        .setGranularity(GRANULARITY_FINE)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMinUpdateIntervalMillis(0L)
        .setMaxUpdateDelayMillis(0L)
        .setMaxUpdateAgeMillis(0L)
        .setWaitForAccurateLocation(false)
        .build()

    private fun getCarModeRequest() = LocationRequest.Builder(1_000L)
        .setGranularity(GRANULARITY_FINE)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMinUpdateIntervalMillis(1_000L)
        .setMaxUpdateDelayMillis(0L)
        .setMaxUpdateAgeMillis(0L)
        .build()

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

    private suspend fun getLastLocation(): Location? {
        return userLocationRepository.getLastLocation()?.let { entity ->
            Location("fused").apply {
                latitude = entity.lat
                longitude = entity.lon
                entity.bearing?.let { bearing = it }
                entity.alt?.let { altitude = it }
                entity.accuracy?.let { accuracy = it }
                entity.speed?.let { speed = it }
                time = entity.lastUpdate
            }
        }
    }

    inner class LocationBinder : Binder() {
        val service: LocationForegroundService
            get() = this@LocationForegroundService
    }

    data class State(
        val foregroundSettings: LocationRequestSettings = LocationRequestSettings(
            interval = 5_000L,
            minInterval = 1_000L,
            maxDelay = 0L,
            maxAge = 5_000L,
        ),
        val backgroundSettings: LocationRequestSettings = LocationRequestSettings(
            interval = 600_000L,
            minInterval = 300_000L,
            maxDelay = 900_000L,
            maxAge = 300_000L
        ),
        val isLocationUpdatesStarted: Boolean = false,
        val isSharingEnabled: Boolean = false,
        val isCharging: Boolean = false,
        val isCarMode: Boolean = false,
        val isAppForeground: Boolean = true,
        val forcedForegroundStatus: LocationService.ForcedForegroundStatus = LocationService.ForcedForegroundStatus(),
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
        const val ACTION_TOGGLE_SHARING = "TOGGLE_SHARING"
        const val ACTION_KILL_SERVICE = "KILL_LOCATION_SERVICE"
    }

}

internal fun FusedLocationProviderClient.locationUpdates(
    request: LocationRequest,
    looper: Looper
): Flow<Location> = callbackFlow {
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { trySend(it) }
        }
    }

    try {
        requestLocationUpdates(request, callback, looper).await()
        awaitClose()
    } catch (exception: SecurityException) {
        close(exception)
    } finally {
        removeLocationUpdates(callback)
    }
}.conflate()
