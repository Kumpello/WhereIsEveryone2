package com.kumpello.whereiseveryone.domain.model

import com.kumpello.whereiseveryone.data.model.map.LocationResponse
import com.kumpello.whereiseveryone.data.model.map.PositionsRequest
import com.kumpello.whereiseveryone.data.model.map.PositionsResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface LocationApi {
    //TODO: Change paths as they are unknown for now

    @HTTP(method = "POST", path = "location/update", hasBody = true)
    fun sendLocation(@Header("Authorization") token:String, @Body requestData: com.kumpello.whereiseveryone.data.model.map.LocationRequest): Call<LocationResponse>

    @HTTP(method = "POST", path = "location/fetch", hasBody = true)
    fun getPositions(@Header("Authorization") token:String, @Body requestData: PositionsRequest): Call<PositionsResponse>
}