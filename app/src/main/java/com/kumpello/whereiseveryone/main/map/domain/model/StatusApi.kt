package com.kumpello.whereiseveryone.main.map.domain.model


import com.kumpello.whereiseveryone.main.map.data.model.StatusRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface StatusApi {
    @HTTP(method = "PUT", path = "me/status", hasBody = true)
    fun updateStatus(@Header("Authorization") token:String, @Body status: StatusRequest): Call<ResponseBody>
}