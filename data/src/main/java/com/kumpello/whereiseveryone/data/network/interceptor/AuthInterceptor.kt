package com.kumpello.whereiseveryone.data.network.interceptor

import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.session.RefreshTokenUseCase
import com.kumpello.whereiseveryone.data.session.LogoutUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthInterceptor(
    private val preferencesManager: PreferencesManager
) : Interceptor, KoinComponent {

    private val refreshTokenUseCase: RefreshTokenUseCase by inject()
    private val logoutUseCase: LogoutUseCase by inject()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val isAuthRequest = originalRequest.url.encodedPath.contains("auth/")

        val requestWithToken = if (!isAuthRequest && originalRequest.header("Authorization") == null) {
            val token = preferencesManager.getCached(PreferencesKey.AuthToken)
            if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
        } else {
            originalRequest
        }

        val response = chain.proceed(requestWithToken)
        val requestToken = requestWithToken.header("Authorization")?.removePrefix("Bearer ")

        if (!isAuthRequest && response.code == 403 && requestWithToken.header("Authorization") != null &&
            response.peekBody(64).string() in listOf("invalid session", "invalid token")) {
            // Resource-level 403 responses are not session revocations.
            if (requestToken != null) {
                runBlocking { logoutUseCase.execute(expectedAccessToken = requestToken) }
            }
            return response
        }

        if (response.code == 401 && !isAuthRequest) {
            synchronized(this) {
                val currentToken = preferencesManager.getCached(PreferencesKey.AuthToken)

                if (currentToken != null && currentToken != requestToken) {
                    response.close()
                    return chain.proceed(
                        requestWithToken.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                    )
                }

                val refreshToken = preferencesManager.getCached(PreferencesKey.AuthRefreshToken)
                if (refreshToken != null) {
                    val authData = runBlocking { refreshTokenUseCase.execute() }
                    if (authData == RefreshTokenUseCase.Response.Success) {
                        val authToken = preferencesManager.getCached(PreferencesKey.AuthToken)
                        response.close()
                        return chain.proceed(
                            requestWithToken.newBuilder()
                                .header("Authorization", "Bearer $authToken")
                                .build()
                        )
                    }
                } else if (requestToken != null) {
                    runBlocking { logoutUseCase.execute(expectedAccessToken = requestToken) }
                }
            }
        }

        return response
    }

}
