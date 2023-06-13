package com.kumpello.whereiseveryone.domain.usecase

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.data.model.map.UserPosition
import com.kumpello.whereiseveryone.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    private val mBinder: IBinder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val CHANNEL_ID = "WhereIsEveryone notification"
    val usersLiveData: MutableLiveData<List<UserPosition>> = MutableLiveData()

    @Inject lateinit var positionsService: PositionsService

    inner class LocalBinder : Binder() {
        val service : LocationService
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
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        //TODO: Add icons and logo
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            //.setSmallIcon(R.drawable.icon)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.ticker_text))
            .build()


        startForeground(420, notification)
    }

    override fun onCreate() {
        startNotification()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            //Start doing shit
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()

        //Do shit on start, there should be start of other users data broadcast

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }

}