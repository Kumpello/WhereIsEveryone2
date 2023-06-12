package com.kumpello.whereiseveryone.domain.usecase

import android.util.Log
import com.kumpello.whereiseveryone.data.model.ErrorData
import com.kumpello.whereiseveryone.data.model.authorization.AuthApi
import com.kumpello.whereiseveryone.data.model.authorization.AuthData
import com.kumpello.whereiseveryone.data.model.authorization.AuthResponse
import com.kumpello.whereiseveryone.data.model.authorization.LogInRequestData
import com.kumpello.whereiseveryone.data.model.authorization.SignUpRequestData
import com.kumpello.whereiseveryone.data.services.RetrofitClient
import dagger.hilt.android.scopes.ViewModelScoped
import retrofit2.Response
import javax.inject.Inject

@ViewModelScoped
class AuthenticationService @Inject constructor() {

    private val retrofit = RetrofitClient.getClient()
    private val authApi = retrofit.create(AuthApi::class.java)

    fun signUp(username: String, password: String): AuthResponse {
        val authResponse = authApi.signUp(SignUpRequestData(username, password)).execute()
        logError(authResponse)
        return if (authResponse.isSuccessful) {
            authResponse.body()!!
        } else {
            ErrorData(authResponse.errorBody()!!)
        }
    }

    fun logIn(username: String, password: String): AuthResponse {
        val authResponse = authApi.login(LogInRequestData(username, password)).execute()
        logError(authResponse)
        return if (authResponse.isSuccessful) {
            authResponse.body()!!
        } else {
            ErrorData(authResponse.errorBody()!!)
        }
    }

    private fun logError(response: Response<AuthData>) {
        if (!response.isSuccessful) {
            Log.e("Authentication error: ", response.errorBody().toString())
        } else {
            Log.d("Authentication:", response.body().toString())
        }
    }

}