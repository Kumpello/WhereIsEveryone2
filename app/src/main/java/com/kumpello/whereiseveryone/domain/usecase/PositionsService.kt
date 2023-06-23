package com.kumpello.whereiseveryone.domain.usecase

import android.util.Log
import com.kumpello.whereiseveryone.data.model.ErrorData
import com.kumpello.whereiseveryone.data.model.map.LocationRequest
import com.kumpello.whereiseveryone.data.model.map.LocationResponse
import com.kumpello.whereiseveryone.data.model.map.PositionsRequest
import com.kumpello.whereiseveryone.data.model.map.PositionsResponse
import com.kumpello.whereiseveryone.data.model.map.Response
import com.kumpello.whereiseveryone.data.services.RetrofitClient
import com.kumpello.whereiseveryone.domain.model.LocationApi
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