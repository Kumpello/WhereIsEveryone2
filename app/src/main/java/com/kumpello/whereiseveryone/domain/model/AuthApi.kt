package com.kumpello.whereiseveryone.domain.model

import com.kumpello.whereiseveryone.data.model.authorization.AuthData
import com.kumpello.whereiseveryone.data.model.authorization.LogInRequest
import com.kumpello.whereiseveryone.data.model.authorization.SignUpRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP

interface AuthApi {
    @HTTP(method = "POST", path = "auth/signup", hasBody = true)
    fun signUp(@Body requestData: SignUpRequest): Call<AuthData>

    @HTTP(method = "POST", path = "auth/login", hasBody = true)
    fun login(@Body requestData: LogInRequest): Call<AuthData>
}