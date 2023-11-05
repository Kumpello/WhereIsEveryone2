package com.kumpello.whereiseveryone.map.domain.repository

import android.util.Log
import com.kumpello.whereiseveryone.common.data.model.ErrorData
import com.kumpello.whereiseveryone.map.data.model.LocationRequest
import com.kumpello.whereiseveryone.map.data.model.LocationResponse
import com.kumpello.whereiseveryone.map.data.model.PositionsRequest
import com.kumpello.whereiseveryone.map.data.model.PositionsResponse
import com.kumpello.whereiseveryone.map.data.model.Response
import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient
import com.kumpello.whereiseveryone.common.domain.model.LocationApi
import dagger.hilt.android.scopes.ServiceScoped
import okhttp3.ResponseBody
import javax.inject.Inject

@ServiceScoped
class PositionsService @Inject constructor() {

    private val retrofit = RetrofitClient.getClient()
    private val locationApi = retrofit.create(LocationApi::class.java)

    fun sendLocation(token: String, longitude: Double, latitude: Double): Response {
        val response = locationApi.sendLocation("Bearer: $token", LocationRequest(longitude, latitude)).execute()
        return if (response.isSuccessful) {
            LocationResponse(response.code())
        } else {
            logError(response.errorBody())
            ErrorData(response.code(), response.errorBody().toString(), response.message())
        }
    }

    fun getPositions(token: String, users: List<String>, uuids: List<String>): Response {
        val response = locationApi.getPositions("Bearer: $token", PositionsRequest(users, uuids)).execute()
        return if (response.isSuccessful) {
            PositionsResponse(response.body()!!.positions)
        } else {
            logError(response.errorBody())
            ErrorData(response.code(), response.errorBody().toString(), response.message())
        }
    }

    private fun logError(response: ResponseBody?) {
        Log.e("Location:", response.toString())
    }

}