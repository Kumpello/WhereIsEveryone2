package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.api.LocationApi
import com.kumpello.whereiseveryone.main.map.domain.model.LocationRequest
import timber.log.Timber
import kotlin.time.Instant

class LocationRepositoryImpl(
    private val locationApi: LocationApi
) : LocationRepository {

    override fun sendPosition(
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
        ).execute()
        return when {
            response.isSuccessful -> CodeResponse.SuccessNoContent

            else -> {
                Timber.e(response.errorBody().toString())
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

}