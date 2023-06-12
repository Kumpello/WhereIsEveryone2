package com.kumpello.whereiseveryone.domain.model

import com.kumpello.whereiseveryone.data.model.map.LocationResponse
import com.kumpello.whereiseveryone.data.model.map.PositionsRequest
import com.kumpello.whereiseveryone.data.model.map.PositionsResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP

interface MapApi {
    //TODO: Change paths as they are unknown for now

    @HTTP(method = "POST", path = "map/location", hasBody = true)
    fun sendLocation(@Body requestData: com.kumpello.whereiseveryone.data.model.map.LocationRequest): Call<LocationResponse>

    @HTTP(method = "POST", path = "map/positions", hasBody = true)
    fun getPositions(@Body requestData: PositionsRequest): Call<PositionsResponse>
}