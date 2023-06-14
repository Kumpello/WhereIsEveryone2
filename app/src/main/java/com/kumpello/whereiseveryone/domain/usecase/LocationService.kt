package com.kumpello.whereiseveryone.domain.usecase

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity.GRANULARITY_FINE
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.data.model.map.Response
import com.kumpello.whereiseveryone.data.model.map.UserPosition
import com.kumpello.whereiseveryone.domain.events.GetPositionsEvent
import com.kumpello.whereiseveryone.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    private val mBinder: IBinder = LocalBinder()
    private val CHANNEL_ID = "WhereIsEveryone notification"
    private val MAX_UPDATE_AGE = 300000L
    private val cancellationSource = CancellationTokenSource()
    val UPDATE_INTERVAL_FOREGROUND = 1000 // 1 min
    val UPDATE_INTERVAL_BACKGROUND = 900000 // 15 min
    val usersLiveData: MutableLiveData<List<UserPosition>> = MutableLiveData()
    val event = MutableSharedFlow<GetPositionsEvent>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Inject
    lateinit var positionsService: PositionsService

    @Inject
    lateinit var friendsService: FriendsService
    private var updateInterval = UPDATE_INTERVAL_FOREGROUND
    private var updateLocation = true
    var updateRequest = getForegroundRequest()

    inner class LocalBinder : Binder() {
        val service: LocationService
            get() = this@LocationService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    private fun startNotification() {
        /*        val channel = NotificationChannel(
                    CHANNEL_ID,
                    "WhereIsEveryone Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)*/

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

            while (updateLocation) {
                try {
                    fusedLocationClient.getCurrentLocation(updateRequest, cancellationSource.token)
                        .addOnSuccessListener { location: Location? ->
                            sendLocation(location)
                        }
                } catch (exception: SecurityException) {
                    SystemClock.sleep(15000)
                }
                SystemClock.sleep(updateInterval.toLong())
            }
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }

    fun changeUpdateInterval(interval: Int) {
        updateInterval = interval
    }

    fun stopUpdates() {
        updateLocation = false
    }

    private fun sendLocation(location: Location?) {
        if (location != null) {
            when (val response =
                positionsService.sendLocation(location.longitude, location.latitude).statusCode) {
                in 200..300 -> Log.d("LocationService:", "Sending location code $response")
                else -> Log.w("LocationService:", "Sending location code $response")
            }
        }
    }

    private fun getPositions(): Response {
        val friends =
            friendsService.getFriends(applicationContext).map { Pair(it.nick, it.id) }.unzip()
        return positionsService.getPositions(friends.first, friends.second)
    }

    private fun getForegroundRequest(): CurrentLocationRequest {
        return CurrentLocationRequest.Builder()
            .setGranularity(GRANULARITY_FINE)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_UPDATE_AGE)
            .build()
    }

}