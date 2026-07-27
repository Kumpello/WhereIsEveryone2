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

    override suspend fun signUp(username: String, password: String, deviceToken: String?): AuthResponse {
        val authResponse = authApi.signUp(SignUpRequest(username, password, deviceToken))

        return when {
            authResponse.isSuccessful -> {
                Timber.tag(TAG).d("SignUp successful for user: %s", username)
                authResponse.body()!!
            }

            else -> {
                Timber.tag(TAG).e("SignUp failed: %s", authResponse.errorBody()?.string())
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
                Timber.tag(TAG).d("Login successful for user: %s", username)
                authResponse.body()!!
            }

            else -> {
                Timber.tag(TAG).e("Login failed: %s", authResponse.errorBody()?.string())
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
                Timber.tag(TAG).e("Token refresh failed: %s", authResponse.errorBody()?.string())
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
