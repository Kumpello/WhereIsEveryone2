package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.network.api.LocationApi
import com.kumpello.whereiseveryone.data.network.model.LocationRequest
import com.kumpello.whereiseveryone.data.logging.httpFailure
import timber.log.Timber

internal class LocationRepositoryImpl(
    private val locationApi: LocationApi
) : LocationRepository {

    override suspend fun sendPosition(
        longitude: Double,
        latitude: Double,
        bearing: Float,
        altitude: Double,
        accuracy: Float,
        speed: Float,
        lastUpdate: Long
    ): CodeResponse {
        val response = locationApi.sendLocation(
            LocationRequest(
                longitude = longitude,
                latitude = latitude,
                bearing = bearing,
                altitude = altitude,
                accuracy = accuracy,
                speed = speed,
                last_update = lastUpdate
            )
        )
        return when {
            response.isSuccessful -> {
                Timber.tag(TAG).d("Location sent successfully")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).httpFailure("Send location", response.code())
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

    override suspend fun wipeLocation(): CodeResponse {
        val response = locationApi.wipeLocation()
        return when {
            response.isSuccessful -> {
                Timber.tag(TAG).d("Location wiped successfully")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).httpFailure("Wipe location", response.code())
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