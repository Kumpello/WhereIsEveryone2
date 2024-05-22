package com.kumpello.whereiseveryone.main.map.domain.model

import com.kumpello.whereiseveryone.common.model.ErrorData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface StatusApi {
    @HTTP(method = "PUT", path = "me/status", hasBody = true)
    fun updateStatus(@Header("Authorization") token:String, @Body message: String): Call<ErrorData?>
}