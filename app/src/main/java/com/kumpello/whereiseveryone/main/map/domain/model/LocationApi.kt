package com.kumpello.whereiseveryone.main.map.domain.model

import com.kumpello.whereiseveryone.main.map.data.model.LocationRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface LocationApi {
    @HTTP(method = "PUT", path = "me/location", hasBody = true)
    fun sendLocation(@Header("Authorization") token:String, @Body requestData: LocationRequest): Call<ResponseBody>
}