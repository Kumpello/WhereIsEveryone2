package com.kumpello.whereiseveryone.common.domain.repository

import com.kumpello.whereiseveryone.authentication.login.domain.model.LogInRequest
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.SignUpRequest
import com.kumpello.whereiseveryone.common.domain.model.AuthApi
import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient
import com.kumpello.whereiseveryone.common.model.AuthResponse
import com.kumpello.whereiseveryone.common.model.ErrorData
import timber.log.Timber

class AuthenticationRepositoryImpl : AuthenticationRepository {

    private val retrofit = RetrofitClient.getClient()
    private val authApi = retrofit.create(AuthApi::class.java)

    override fun signUp(username: String, password: String): AuthResponse {
        val authResponse = authApi.signUp(SignUpRequest(username, password)).execute()
        return if (authResponse.isSuccessful) {
            authResponse.body()!!
        } else {
            Timber.e(authResponse.errorBody().toString())
            ErrorData(authResponse.code(), authResponse.errorBody().toString(), authResponse.message())
        }
    }

    override fun logIn(username: String, password: String): AuthResponse {
        val authResponse = authApi.login(LogInRequest(username, password)).execute()
        Timber.d(authResponse.message())
        return if (authResponse.isSuccessful) {
            authResponse.body()!!
        } else {
            Timber.e(authResponse.errorBody().toString())
            ErrorData(authResponse.code(), authResponse.errorBody().toString(), authResponse.message())
        }
    }

/*    override fun getRefreshToken(): RefreshResponse {
        val refreshResponse = authApi.refresh()
    }*/

}