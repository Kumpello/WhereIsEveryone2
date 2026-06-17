package com.kumpello.whereiseveryone.main.map.domain.api

import com.kumpello.whereiseveryone.main.map.domain.model.LocationRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface LocationApi {
    @HTTP(method = "PUT", path = "me/location", hasBody = true)
    suspend fun sendLocation(@Header("Authorization") token:String, @Body requestData: LocationRequest): Response<ResponseBody>
}