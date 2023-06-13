package com.kumpello.whereiseveryone.domain.usecase

import android.util.Log
import com.kumpello.whereiseveryone.data.model.ErrorData
import com.kumpello.whereiseveryone.data.model.authorization.AuthData
import com.kumpello.whereiseveryone.data.model.authorization.AuthResponse
import com.kumpello.whereiseveryone.data.model.authorization.LogInRequest
import com.kumpello.whereiseveryone.data.model.authorization.SignUpRequest
import com.kumpello.whereiseveryone.data.services.RetrofitClient
import com.kumpello.whereiseveryone.domain.model.AuthApi
import dagger.hilt.android.scopes.ViewModelScoped
import retrofit2.Response
import javax.inject.Inject

@ViewModelScoped
class AuthenticationService @Inject constructor() {

    private val retrofit = RetrofitClient.getClient()
    private val authApi = retrofit.create(AuthApi::class.java)

    fun signUp(username: String, password: String): AuthResponse {
        val authResponse = authApi.signUp(SignUpRequest(username, password)).execute()
        return if (authResponse.isSuccessful) {
            authResponse.body()!!
        } else {
            logError(authResponse)
            ErrorData(authResponse.errorBody()!!)
        }
    }

    fun logIn(username: String, password: String): AuthResponse {
        val authResponse = authApi.login(LogInRequest(username, password)).execute()
        logError(authResponse)
        return if (authResponse.isSuccessful) {
            authResponse.body()!!
        } else {
            logError(authResponse)
            ErrorData(authResponse.errorBody()!!)
        }
    }

    private fun logError(response: Response<AuthData>) {
        Log.e("Authentication:", response.errorBody().toString())
    }

}