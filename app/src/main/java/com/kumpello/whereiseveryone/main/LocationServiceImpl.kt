package com.kumpello.whereiseveryone.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity.GRANULARITY_FINE
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kumpello.whereiseveryone.main.MainActivity.Companion.STATUS_PARAM
import com.kumpello.whereiseveryone.main.map.domain.usecase.SendPositionUseCase
import org.koin.android.ext.android.inject
import timber.log.Timber

class LocationServiceImpl : Service(), LocationService {


    //TODO: Add State and initialization
    private val binder: IBinder = LocationBinder()
    private val channelID = "WhereIsEveryone notification"
    private val maxUpdateAge = 300000L // 5 min
    private val cancellationSource = CancellationTokenSource()
    private var updateLocationIntervalForeground = 1000 // 1 min
    private var updateLocationIntervalBackground = 900000 // 15 min
    private val sendPositionUseCase: SendPositionUseCase by inject()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var updateInterval = updateLocationIntervalForeground
    private var updateLocation = true

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.d("LocationService starting")

        val input = intent.getStringExtra(STATUS_PARAM)
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        //TODO: Add icons and logo
        val notification: Notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle(getText(com.kumpello.whereiseveryone.R.string.notification_title))
            .setContentText(input)
            //.setSmallIcon(R.drawable.ic_stat_name)
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
            startLocationUpdates()
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
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

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            channelID,
            "WhereIsEveryone Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun startLocationUpdates() {
        while (updateLocation) {
            try {
                Timber.d("Trying to send location")
                fusedLocationClient.getCurrentLocation(getForegroundRequest(), cancellationSource.token)
                    .addOnSuccessListener { location: Location? ->
                        Timber.d("Sending location")
                        sendLocation(location)
                    }
            } catch (exception: SecurityException) {
                SystemClock.sleep(15000)
                Timber.e(exception.toString())
            }
            SystemClock.sleep(updateInterval.toLong()) //TODO: Do this in non shitty way via Job Scheduler
        }
    }

    fun stopUpdates() {
        updateLocation = false
    }

    private fun sendLocation(location: Location?) {
        if (location != null) {
            sendPositionUseCase.execute(location.longitude, location.latitude)
                .let { response ->
                    Timber.d("Sending location code $response")
                }
        }
    }

    private fun getForegroundRequest() = CurrentLocationRequest.Builder()
        .setGranularity(GRANULARITY_FINE)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMaxUpdateAgeMillis(maxUpdateAge)
        .build()

    override fun changeForegroundUpdateInterval(interval: Int) {
        updateLocationIntervalForeground = interval
    }

    override fun changeBackgroundUpdateInterval(interval: Int) {
        updateLocationIntervalBackground = interval
    }

    override fun setUpdateInterval(type: LocationService.UpdateInterval) {
        updateInterval = when(type) {
            LocationService.UpdateInterval.Background -> updateLocationIntervalBackground
            LocationService.UpdateInterval.Foreground -> updateLocationIntervalForeground
        }
    }

}