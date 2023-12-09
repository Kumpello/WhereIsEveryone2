package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.LocationApi
import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient
import com.kumpello.whereiseveryone.main.map.data.model.LocationRequest
import com.kumpello.whereiseveryone.main.map.data.model.LocationResponse
import com.kumpello.whereiseveryone.main.map.data.model.PositionsRequest
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import timber.log.Timber

class PositionsService {

    private val retrofit = RetrofitClient.getClient()
    private val locationApi = retrofit.create(LocationApi::class.java)

    fun sendLocation(token: String, longitude: Double, latitude: Double): LocationResponse {
        val response = locationApi.sendLocation("Bearer: $token", LocationRequest(longitude, latitude)).execute()
        return LocationResponse(response.code()).also {
            if (response.isSuccessful.not()) {
                Timber.e(response.toString())
            }
        }
    }

    fun getPositions(token: String, users: List<String>, uuids: List<String>): PositionsResponse {
        val response = locationApi.getPositions("Bearer: $token", PositionsRequest(users, uuids)).execute()
        return PositionsResponse(response.body()?.positions).also {
            if (response.isSuccessful.not()) {
                Timber.e(response.toString())
            }
        }
    }

}