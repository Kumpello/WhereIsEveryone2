package com.kumpello.whereiseveryone.main.map.presentation

import android.location.Location
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import timber.log.Timber
import java.io.IOException
import java.net.ProtocolException
import javax.net.ssl.SSLException
import kotlin.time.Duration.Companion.milliseconds

internal data class LocationUpload(
    val location: Location,
    val lastUpdate: Long
)

internal suspend fun sendLocationWithRetry(
    initialUpload: LocationUpload,
    takeLatestUpload: () -> LocationUpload?,
    send: suspend (LocationUpload) -> CodeResponse
): CodeResponse {
    var upload = initialUpload
    var retryDelayMs = 5_000L

    for (attempt in 1..3) {
        currentCoroutineContext().ensureActive()
        try {
            val response = send(upload)
            val shouldRetry = response is CodeResponse.ErrorData &&
                response.code in setOf(408, 500, 502, 503, 504)
            if (!shouldRetry || attempt == 3) return response
            Timber.tag("LOCATION_SERVICE").d("Location upload attempt %d rejected; will retry", attempt)
        } catch (exception: IOException) {
            if (attempt == 3 || exception is ProtocolException || exception is SSLException) {
                throw exception
            }
            Timber.tag("LOCATION_SERVICE").d(exception, "Location upload attempt %d failed; will retry", attempt)
        }

        delay(retryDelayMs.milliseconds)
        retryDelayMs *= 2
        // Keep the failure budget and backoff when a fresher fix replaces the failed upload.
        upload = takeLatestUpload() ?: upload
    }

    error("Location upload retry limit exhausted")
}
