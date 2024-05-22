package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient
import com.kumpello.whereiseveryone.main.map.data.model.LocationRequest
import com.kumpello.whereiseveryone.main.map.data.model.LocationResponse
import com.kumpello.whereiseveryone.main.map.domain.model.LocationApi
import timber.log.Timber

class LocationRepositoryImpl : LocationRepository {

    private val retrofit = RetrofitClient.getClient()
    private val locationApi = retrofit.create(LocationApi::class.java)

    override fun sendPosition(token: String, longitude: Double, latitude: Double): LocationResponse {
        val response = locationApi.sendLocation("Bearer: $token", LocationRequest(longitude, latitude)).execute()
        return LocationResponse(response.code()).also {
            if (response.isSuccessful.not()) {
                Timber.e(response.toString())
            }
        }
    }

}