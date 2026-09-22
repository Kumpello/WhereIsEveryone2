package com.kumpello.whereiseveryone.common.domain.repository

import com.kumpello.whereiseveryone.authentication.login.domain.model.LogInRequest
import com.kumpello.whereiseveryone.authentication.login.domain.model.RefreshRequest
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.SignUpRequest
import com.kumpello.whereiseveryone.common.domain.model.AuthApi
import com.kumpello.whereiseveryone.common.model.AuthResponse
import com.kumpello.whereiseveryone.common.logging.httpFailure
import timber.log.Timber

class AuthenticationRepositoryImpl(
    private val authApi: AuthApi
) : AuthenticationRepository {

    override suspend fun signUp(username: String, password: String, deviceToken: String?): AuthResponse {
        val authResponse = authApi.signUp(SignUpRequest(username, password, deviceToken))

        return when {
            authResponse.isSuccessful -> {
                Timber.tag(TAG).d("SignUp successful")
                authResponse.body()!!
            }

            else -> {
                Timber.tag(TAG).httpFailure("SignUp", authResponse.code())
                AuthResponse.ErrorData(
                    authResponse.code(),
                    authResponse.errorBody().toString(),
                    authResponse.message()
                )
            }
        }
    }

    override suspend fun logIn(username: String, password: String, deviceToken: String?): AuthResponse {
        val authResponse = authApi.login(LogInRequest(username, password, deviceToken))

        return when {
            authResponse.isSuccessful -> {
                Timber.tag(TAG).d("Login successful")
                authResponse.body()!!
            }

            else -> {
                Timber.tag(TAG).httpFailure("Login", authResponse.code())
                AuthResponse.ErrorData(
                    authResponse.code(),
                    authResponse.errorBody().toString(),
                    authResponse.message()
                )
            }
        }
    }

    override suspend fun refreshToken(refreshToken: String, deviceToken: String?): AuthResponse {
        val authResponse = authApi.refresh(RefreshRequest(refreshToken, deviceToken))

        return when {
            authResponse.isSuccessful -> {
                Timber.tag(TAG).d("Token refresh successful")
                authResponse.body()!!
            }

            else -> {
                Timber.tag(TAG).httpFailure("Token refresh", authResponse.code())
                AuthResponse.ErrorData(
                    authResponse.code(),
                    authResponse.errorBody().toString(),
                    authResponse.message()
                )
            }
        }
    }

    companion object {
        private const val TAG = "AUTH_REPO"
    }

}
