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
        return try {
            val authResponse = authApi.signUp(SignUpRequest(username, password))

            when {
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
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "SignUp failed with exception")
            AuthResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    override suspend fun logIn(username: String, password: String): AuthResponse {
        return try {
            val authResponse = authApi.login(LogInRequest(username, password))

            when {
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
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Login failed with exception")
            AuthResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    override suspend fun refreshToken(refreshToken: String): AuthResponse {
        return try {
            val authResponse = authApi.refresh(RefreshRequest(refreshToken))

            when {
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
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Token refresh failed with exception")
            AuthResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    companion object {
        private const val TAG = "AUTH_REPO"
    }

}
