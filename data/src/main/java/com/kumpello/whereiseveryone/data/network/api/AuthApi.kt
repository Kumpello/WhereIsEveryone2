package com.kumpello.whereiseveryone.data.network.api

import com.kumpello.whereiseveryone.data.network.model.AuthResponse
import com.kumpello.whereiseveryone.data.network.model.LogInRequest
import com.kumpello.whereiseveryone.data.network.model.RefreshRequest
import com.kumpello.whereiseveryone.data.network.model.SignUpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP

internal interface AuthApi {
    @HTTP(method = "POST", path = "auth/signup", hasBody = true)
    suspend fun signUp(@Body requestData: SignUpRequest): Response<AuthResponse.AuthData>

    @HTTP(method = "POST", path = "auth/login", hasBody = true)
    suspend fun login(@Body requestData: LogInRequest): Response<AuthResponse.AuthData>

    @HTTP(method = "POST", path = "auth/refresh", hasBody = true)
    suspend fun refresh(@Body requestData: RefreshRequest): Response<AuthResponse.AuthData>
}