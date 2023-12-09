package com.kumpello.whereiseveryone.main.map.domain.repository

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import android.widget.Toast
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity.GRANULARITY_FINE
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.main.MainActivity
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class LocationService : Service() {

    private val mBinder: IBinder = LocalBinder()
    private val CHANNEL_ID = "WhereIsEveryone notification"
    private val MAX_UPDATE_AGE = 300000L // 5 min
    private val cancellationSource = CancellationTokenSource()
    val UPDATE_LOCATION_INTERVAL_FOREGROUND = 1000 // 1 min
    val UPDATE_LOCATION_INTERVAL_BACKGROUND = 900000 // 15 min
    val UPDATE_FRIENDS_INTERVAL = 3000 // 3 min
    val event = MutableSharedFlow<PositionsResponse>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var token: String

    private lateinit var positionsService: PositionsService
    private val getKeyUseCase: GetKeyUseCase by inject()

    private var updateInterval = UPDATE_LOCATION_INTERVAL_FOREGROUND
    private var updateLocation = true
    private var updateFriends = true
    var updateRequest = getForegroundRequest()

    inner class LocalBinder : Binder() {
        val service: LocationService
            get() = this@LocationService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    private fun startNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "WhereIsEveryone Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        //TODO: Add icons and logo
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            //.setSmallIcon(R.drawable.icon)
            .setContentIntent(pendingIntent)
            //.setTicker(getText(R.string.ticker_text))
            .build()

        startForeground(420, notification)
    }

    override fun onCreate() {
        startNotification()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()

        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
            startLocationUpdates()
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }

    fun setCurrentToken(token: String) {
        this.token = token
    }

    private fun startLocationUpdates() {
        while (updateLocation) {
            try {
                Timber.d("Trying to send location")
                fusedLocationClient.getCurrentLocation(updateRequest, cancellationSource.token)
                    .addOnSuccessListener { location: Location? ->
                        Timber.d("Sending location")
                        sendLocation(location)
                    }
            } catch (exception: SecurityException) {
                SystemClock.sleep(15000)
                Timber.e(exception.toString())
            }
            SystemClock.sleep(updateInterval.toLong())
        }
    }

    fun startFriendsUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            while (updateFriends) {
                Timber.d("Trying to get location")
                val positions = getPositions()
                Timber.d("emiting friends location")
                event.emit(positions)
                delay(UPDATE_FRIENDS_INTERVAL.toLong())
            }
        }
    }

    fun setUpdateInterval(interval: Int) {
        updateInterval = interval
    }

    fun stopUpdates() {
        updateLocation = false
    }

    fun stopFriendsUpdates() {
        updateFriends = false
    }

    private fun sendLocation(location: Location?) {
        if (location != null) {
            positionsService
                .sendLocation(token, location.longitude, location.latitude)
                .let { response ->
                    Timber.d("Sending location code $response")
                }
        }
    }

    private fun getPositions(): PositionsResponse {
        val friends =
            getKeyUseCase.getFriends()
                .map { Pair(it.nick, it.id) }.unzip()
        return positionsService.getPositions(token, friends.first, friends.second)
    }

    private fun getForegroundRequest(): CurrentLocationRequest {
        return CurrentLocationRequest.Builder()
            .setGranularity(GRANULARITY_FINE)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_UPDATE_AGE)
            .build()
    }

}