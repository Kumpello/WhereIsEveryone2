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
        return try {
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
            when {
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
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error sending location with exception")
            CodeResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    override suspend fun wipeLocation(token: String): CodeResponse {
        return try {
            val response = locationApi.wipeLocation(
                "Bearer $token"
            )
            when {
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
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error wiping location with exception")
            CodeResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    companion object {
        private const val TAG = "LOCATION_REPO"
    }

}