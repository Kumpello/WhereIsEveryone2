package com.kumpello.whereiseveryone.main.map.domain.api


import com.kumpello.whereiseveryone.main.map.domain.model.StatusRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP

interface StatusApi {
    @HTTP(method = "PUT", path = "me/status", hasBody = true)
    suspend fun updateStatus(@Body status: StatusRequest): Response<ResponseBody>
}