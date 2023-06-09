package com.kumpello.whereiseveryone.domain.usecase

import android.util.Log
import com.kumpello.whereiseveryone.data.model.authorization.AuthApi
import com.kumpello.whereiseveryone.data.model.authorization.AuthData
import com.kumpello.whereiseveryone.data.model.authorization.LogInRequestData
import com.kumpello.whereiseveryone.data.model.authorization.SignUpRequestData
import com.kumpello.whereiseveryone.data.services.RetrofitClient
import dagger.hilt.android.scopes.ViewModelScoped
import retrofit2.Response
import java.util.Optional
import javax.inject.Inject

@ViewModelScoped
class AuthenticationService @Inject constructor() {

    private val retrofit = RetrofitClient.getClient()
    private val authApi = retrofit.create(AuthApi::class.java)

    fun signUp(username: String, email: String, password: String): Optional<AuthData> {
        val authResponse = authApi.signUp(SignUpRequestData(username, email, password)).execute()
        logError(authResponse)
        return Optional.ofNullable(authResponse.body())
    }

    fun logIn(username: String, password: String): Optional<AuthData> {
        val authResponse = authApi.login(LogInRequestData(username, password)).execute()
        logError(authResponse)
        return Optional.ofNullable(authResponse.body())
    }

    private fun logError(response: Response<AuthData>) {
        if (!response.isSuccessful) {
            Log.e("Authentication error: ", response.errorBody().toString())
        } else {
            Log.d("Authentication:", response.body().toString())
        }
    }

}