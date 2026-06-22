package com.kumpello.whereiseveryone.common.domain.services

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.RefreshTokenUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val getKeyUseCase: GetKeyUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val isAuthRequest = originalRequest.url.encodedPath.contains("auth/")

        val requestWithToken = if (!isAuthRequest && originalRequest.header("Authorization") == null) {
            val token = runBlocking { getKeyUseCase.getValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY) }
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
                val currentToken = runBlocking { getKeyUseCase.getValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY) }
                val requestToken = requestWithToken.header("Authorization")?.removePrefix("Bearer ")

                if (currentToken != null && currentToken != requestToken) {
                    response.close()
                    return chain.proceed(
                        requestWithToken.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                    )
                }

                val refreshToken = runBlocking { getKeyUseCase.getValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY) }
                if (refreshToken != null) {
                    val authData = runBlocking { refreshTokenUseCase.execute() }
                    if (authData == RefreshTokenUseCase.Response.Success) {
                        val authToken = runBlocking { getKeyUseCase.getValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY) }
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
