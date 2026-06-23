package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.api.LocationApi
import com.kumpello.whereiseveryone.main.map.domain.model.LocationRequest
import timber.log.Timber
import kotlin.time.Instant

class LocationRepositoryImpl(
    private val locationApi: LocationApi
) : LocationRepository {

    override suspend fun sendPosition(
        token: String,
        longitude: Double,
        latitude: Double,
        bearing: Float,
        altitude: Double,
        accuracy: Float,
        lastUpdate: Instant
    ): CodeResponse {
        val response = locationApi.sendLocation(
            "Bearer $token", LocationRequest(
                longitude = longitude,
                latitude = latitude,
                bearing = bearing,
                altitude = altitude,
                accuracy = accuracy,
                lastUpdate = lastUpdate
            )
        )
        return when {
            response.isSuccessful -> {
                Timber.tag(TAG).d("Location sent successfully")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).e("Error sending location: %s", response.errorBody()?.string())
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

    override suspend fun wipeLocation(token: String): CodeResponse {
        val response = locationApi.wipeLocation(
            "Bearer $token"
        )
        return when {
            response.isSuccessful -> {
                Timber.tag(TAG).d("Location wiped successfully")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).e("Error wiping location: %s", response.errorBody()?.string())
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

    companion object {
        private const val TAG = "LOCATION_REPO"
    }

}