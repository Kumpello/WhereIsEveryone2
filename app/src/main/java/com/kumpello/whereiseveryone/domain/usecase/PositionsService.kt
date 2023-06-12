package com.kumpello.whereiseveryone.domain.usecase

import android.util.Log
import com.kumpello.whereiseveryone.data.model.ErrorData
import com.kumpello.whereiseveryone.data.model.map.LocationRequest
import com.kumpello.whereiseveryone.data.model.map.LocationResponse
import com.kumpello.whereiseveryone.data.model.map.PositionsRequest
import com.kumpello.whereiseveryone.data.model.map.PositionsResponse
import com.kumpello.whereiseveryone.data.model.map.Response
import com.kumpello.whereiseveryone.data.services.RetrofitClient
import com.kumpello.whereiseveryone.domain.model.MapApi
import dagger.hilt.android.scopes.ViewModelScoped
import okhttp3.ResponseBody
import javax.inject.Inject

@ViewModelScoped
class PositionsService @Inject constructor() {

    private val retrofit = RetrofitClient.getClient()
    private val mapApi = retrofit.create(MapApi::class.java)

    fun sendLocation(longitude: Double, latitude: Double): LocationResponse {
        val response = mapApi.sendLocation(LocationRequest(longitude, latitude)).execute()
        return if (response.isSuccessful) {
            LocationResponse(response.code())
        } else {
            logError(response.errorBody())
            LocationResponse(response.code())
        }
    }

    fun getPositions(users: List<String>, uuids: List<String>): Response {
        val response = mapApi.getPositions(PositionsRequest(users, uuids)).execute()
        return if (response.isSuccessful) {
            PositionsResponse(response.body()!!.positions)
        } else {
            logError(response.errorBody())
            ErrorData(response.errorBody()!!)
        }
    }

    private fun logError(response: ResponseBody?) {
        Log.e("Location:", response.toString())
    }

}