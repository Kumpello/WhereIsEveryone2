package com.kumpello.whereiseveryone.common.domain.model

import com.kumpello.whereiseveryone.main.map.data.model.LocationResponse
import com.kumpello.whereiseveryone.main.map.data.model.PositionsRequest
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import com.kumpello.whereiseveryone.main.map.data.model.LocationRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface LocationApi {
    //TODO: Change paths as they are unknown for now

    @HTTP(method = "POST", path = "location/update", hasBody = true)
    fun sendLocation(@Header("Authorization") token:String, @Body requestData: LocationRequest): Call<LocationResponse>

    @HTTP(method = "POST", path = "location/fetch", hasBody = true)
    fun getPositions(@Header("Authorization") token:String, @Body requestData: PositionsRequest): Call<PositionsResponse>
}