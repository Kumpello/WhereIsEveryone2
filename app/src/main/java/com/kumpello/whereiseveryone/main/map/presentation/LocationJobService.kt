package com.kumpello.whereiseveryone.main.map.presentation;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Handler;
import android.os.HandlerThread;
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.kumpello.whereiseveryone.main.friends.model.Location

import timber.log.Timber;

class LocationJobService(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val getForegroundRequest: CurrentLocationRequest
): JobService() {

    private val TAG: String = LocationJobService.class.getSimpleName();

    override fun onStartJob(p0: JobParameters?): Boolean {

        val handlerThread = HandlerThread("SomeOtherThread")
        handlerThread.start()

        val handler = Handler(handlerThread.looper);

        handler.post {
            Timber.tag(TAG).e("Running!!!!!!!!!!!!!");
            try {
                Timber.d("Trying to send location")
                fusedLocationClient.getCurrentLocation(
                    getForegroundRequest,
                    cancellationSource.token
                )
                    .addOnSuccessListener { location: Location? ->
                        Timber.d("Sending location")
                        sendLocation(location)
                    }
            } catch (exception: SecurityException) {
                SystemClock.sleep(15000)
                Timber.e(exception.toString())
            }

            jobFinished(params, true);
        }

        return true;
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        Timber.tag(TAG).d("onStopJob() was called");
        return true;
    }
}