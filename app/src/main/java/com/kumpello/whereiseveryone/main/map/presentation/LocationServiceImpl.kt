package com.kumpello.whereiseveryone.main.map.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
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
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kumpello.whereiseveryone.main.MainActivity
import com.kumpello.whereiseveryone.main.MainActivity.Companion.STATUS_PARAM
import com.kumpello.whereiseveryone.main.map.domain.usecase.SendPositionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject
import timber.log.Timber


class LocationServiceImpl(
    private val fusedLocationClient: FusedLocationProviderClient
) : Service(), LocationService {

    private val state = MutableStateFlow(State())
    private val locationFlow = MutableSharedFlow<Location>()

    private val binder: IBinder = LocationBinder()
    private val channelID = "WhereIsEveryone"
    private val cancellationSource = CancellationTokenSource()
    private val sendPositionUseCase: SendPositionUseCase by inject()

    private val jobID = 420


    override fun onCreate() {
        super.onCreate()
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
        while (state.value.updateLocation) { //TODO: It doesn't look right, change the way it works
            val jobInfo = JobInfo.Builder(
                jobID,
                ComponentName(this, LocationJobService::class.java)

            )
                .setRequiresBatteryNotLow(state.value.requiresBatteryNotLow)
                .setPeriodic((state.value.updateInterval).toLong())
                .build()
            val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(jobInfo)
            /////////
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
        state.value = state.value.copy(
            updateLocation = false
        )
    }

    private fun sendLocation(location: Location?) {
        location?.let {
            sendPositionUseCase.execute(location.longitude, location.latitude)
                .let { response ->
                    Timber.d("Sending location code $response")
                }
        }
    }

    private fun getForegroundRequest() = CurrentLocationRequest.Builder()
        .setGranularity(GRANULARITY_FINE)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMaxUpdateAgeMillis(state.value.maxUpdateAge)
        .build()

    override fun changeForegroundUpdateInterval(interval: Int) {
        state.value = state.value.copy(
            updateLocationIntervalForeground = interval
        )
    }

    override fun changeBackgroundUpdateInterval(interval: Int) {
        state.value = state.value.copy(
            updateLocationIntervalBackground = interval
        )
    }

    override fun setUpdateInterval(type: LocationService.UpdateInterval) {
        state.value = state.value.copy(
            updateInterval = when (type) {
                LocationService.UpdateInterval.Background -> state.value.updateLocationIntervalBackground
                LocationService.UpdateInterval.Foreground -> state.value.updateLocationIntervalForeground
            }
        )
    }

    override fun getLocation(): Flow<Location> {
        return locationFlow
    }

    data class State(
        val maxUpdateAge: Long = 300000L, //5min
        val updateLocationIntervalForeground: Int = 1000, // 1 min
        val updateLocationIntervalBackground: Int = 900000, // 15 min
        val updateInterval: Int = updateLocationIntervalForeground,
        val requiresBatteryNotLow: Boolean = false,
        val updateLocation: Boolean = true
    )
}