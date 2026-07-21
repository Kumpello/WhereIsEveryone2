package com.kumpello.whereiseveryone.common.domain.services

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.usecase.RefreshTokenUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthInterceptor(
    private val preferencesManager: PreferencesManager
) : Interceptor, KoinComponent {

    private val refreshTokenUseCase: RefreshTokenUseCase by inject()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val isAuthRequest = originalRequest.url.encodedPath.contains("auth/")

        val requestWithToken = if (!isAuthRequest && originalRequest.header("Authorization") == null) {
            val token = runBlocking { preferencesManager.get(PreferencesKey.AuthToken) }
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

        if (response.code == 401 && !isAuthRequest) {
            synchronized(this) {
                val currentToken = runBlocking { preferencesManager.get(PreferencesKey.AuthToken) }
                val requestToken = requestWithToken.header("Authorization")?.removePrefix("Bearer ")

                if (currentToken != null && currentToken != requestToken) {
                    response.close()
                    return chain.proceed(
                        requestWithToken.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                    )
                }

                val refreshToken = runBlocking { preferencesManager.get(PreferencesKey.AuthRefreshToken) }
                if (refreshToken != null) {
                    val authData = runBlocking { refreshTokenUseCase.execute() }
                    if (authData == RefreshTokenUseCase.Response.Success) {
                        val authToken = runBlocking { preferencesManager.get(PreferencesKey.AuthToken) }
                        response.close()
                        return chain.proceed(
                            requestWithToken.newBuilder()
                                .header("Authorization", "Bearer $authToken")
                                .build()
                        )
                    }
                }
            }
        }

        return response
    }

}
