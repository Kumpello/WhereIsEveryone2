package com.kumpello.whereiseveryone.data.model.authorization

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP

interface AuthApi {
    @HTTP(method = "POST", path = "auth/signup", hasBody = true)
    fun signUp(@Body requestData: SignUpRequestData): Call<AuthData>

    @HTTP(method = "POST", path = "auth/login", hasBody = true)
    fun login(@Body requestData: LogInRequestData): Call<AuthData>
}