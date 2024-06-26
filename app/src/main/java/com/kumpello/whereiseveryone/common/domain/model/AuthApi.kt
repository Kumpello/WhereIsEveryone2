package com.kumpello.whereiseveryone.common.domain.model

import com.kumpello.whereiseveryone.common.model.AuthResponse
import com.kumpello.whereiseveryone.authentication.login.domain.model.LogInRequest
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.SignUpRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP

interface AuthApi {
    @HTTP(method = "POST", path = "auth/signup", hasBody = true)
    fun signUp(@Body requestData: SignUpRequest): Call<AuthResponse.AuthData>

    @HTTP(method = "POST", path = "auth/login", hasBody = true)
    fun login(@Body requestData: LogInRequest): Call<AuthResponse.AuthData>

    @HTTP(method = "GET", path = "auth/refresh", hasBody = true)
    fun refresh(@Body requestData: LogInRequest): Call<AuthResponse.AuthData>
}