package com.kumpello.whereiseveryone.common.domain.model

import com.kumpello.whereiseveryone.common.model.AuthResponse
import com.kumpello.whereiseveryone.authentication.login.domain.model.LogInRequest
import com.kumpello.whereiseveryone.authentication.login.domain.model.RefreshRequest
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.SignUpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP

interface AuthApi {
    @HTTP(method = "POST", path = "auth/signup", hasBody = true)
    suspend fun signUp(@Body requestData: SignUpRequest): Response<AuthResponse.AuthData>

    @HTTP(method = "POST", path = "auth/login", hasBody = true)
    suspend fun login(@Body requestData: LogInRequest): Response<AuthResponse.AuthData>

    @HTTP(method = "POST", path = "auth/refresh", hasBody = true)
    suspend fun refresh(@Body requestData: RefreshRequest): Response<AuthResponse.AuthData>
}