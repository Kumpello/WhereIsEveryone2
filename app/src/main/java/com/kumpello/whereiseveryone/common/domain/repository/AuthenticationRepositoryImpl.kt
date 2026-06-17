package com.kumpello.whereiseveryone.common.domain.repository

import com.kumpello.whereiseveryone.authentication.login.domain.model.LogInRequest
import com.kumpello.whereiseveryone.authentication.login.domain.model.RefreshRequest
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.SignUpRequest
import com.kumpello.whereiseveryone.common.domain.model.AuthApi
import com.kumpello.whereiseveryone.common.model.AuthResponse
import timber.log.Timber

class AuthenticationRepositoryImpl(
    private val authApi: AuthApi
) : AuthenticationRepository {

    override suspend fun signUp(username: String, password: String): AuthResponse {
        val authResponse = authApi.signUp(SignUpRequest(username, password))

        return when {
            authResponse.isSuccessful -> authResponse.body()!!

            else -> {
                Timber.e(authResponse.errorBody().toString())
                AuthResponse.ErrorData(
                    authResponse.code(),
                    authResponse.errorBody().toString(),
                    authResponse.message()
                )
            }
        }
    }

    override suspend fun logIn(username: String, password: String): AuthResponse {
        val authResponse = authApi.login(LogInRequest(username, password))
        Timber.d(authResponse.message())

        return when {
            authResponse.isSuccessful -> authResponse.body()!!

            else -> {
                Timber.e(authResponse.errorBody().toString())
                AuthResponse.ErrorData(
                    authResponse.code(),
                    authResponse.errorBody().toString(),
                    authResponse.message()
                )
            }
        }
    }

    override suspend fun refreshToken(refreshToken: String): AuthResponse {
        val authResponse = authApi.refresh(RefreshRequest(refreshToken))
        Timber.d(authResponse.message())

        return when {
            authResponse.isSuccessful -> authResponse.body()!!

            else -> {
                Timber.e(authResponse.errorBody().toString())
                AuthResponse.ErrorData(
                    authResponse.code(),
                    authResponse.errorBody().toString(),
                    authResponse.message()
                )
            }
        }
    }

}