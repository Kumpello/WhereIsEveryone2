package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.SuccessResponse
import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient
import com.kumpello.whereiseveryone.common.model.ErrorData
import com.kumpello.whereiseveryone.main.map.data.model.LocationRequest
import com.kumpello.whereiseveryone.main.map.domain.model.LocationApi
import timber.log.Timber

class LocationRepositoryImpl : LocationRepository {

    private val retrofit = RetrofitClient.getClient()
    private val locationApi = retrofit.create(LocationApi::class.java)

    override fun sendPosition(
        token: String,
        longitude: Double,
        latitude: Double,
        bearing: Float,
        altitude: Double,
        accuracy: Float
    ): SuccessResponse {
        val response = locationApi.sendLocation(
            "Bearer: $token", LocationRequest(
                longitude = longitude,
                latitude = latitude,
                bearing = bearing,
                altitude = altitude,
                accuracy = accuracy
            )
        ).execute()
        return if (response.isSuccessful) {
            SuccessResponse.SuccessData(response.code())
        } else {
            Timber.e(response.errorBody().toString())
            ErrorData(response.code(), response.errorBody().toString(), response.message())
        }
    }

}