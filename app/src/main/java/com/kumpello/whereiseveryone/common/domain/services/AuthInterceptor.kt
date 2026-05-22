package com.kumpello.whereiseveryone.common.domain.services

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.authentication.login.domain.model.RefreshRequest
import com.kumpello.whereiseveryone.common.domain.model.AuthApi
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.model.AuthResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

//TODO To review and improve
class AuthInterceptor(
    private val getKeyUseCase: GetKeyUseCase,
    private val saveKeyUseCase: SaveKeyUseCase
) : Interceptor {

    private val TEMP_BASE_URL = "http://192.168.1.216:8080/api/"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. Skip for auth endpoints to avoid loops and because they handle their own errors
        val isAuthRequest = originalRequest.url.encodedPath.contains("auth/")

        // 2. Add Authorization header if missing and it's NOT an auth request
        val requestWithToken = if (!isAuthRequest && originalRequest.header("Authorization") == null) {
            val token = getKeyUseCase.getValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY)
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

        // 3. Handle 401 if it's NOT an auth request
        if (response.code == 401 && !isAuthRequest) {
            synchronized(this) {
                val currentToken = getKeyUseCase.getValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY)
                val requestToken = requestWithToken.header("Authorization")?.removePrefix("Bearer ")

                // If token was already updated by another thread, retry with new token
                if (currentToken != null && currentToken != requestToken) {
                    response.close()
                    return chain.proceed(
                        requestWithToken.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                    )
                }

                val refreshToken = getKeyUseCase.getValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY)
                if (refreshToken != null) {
                    val authData = refreshAccessToken(refreshToken)
                    if (authData != null) {
                        saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY, authData.token)
                        saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY, authData.refresh_token)
                        saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_ID_KEY, authData.id)

                        response.close()
                        return chain.proceed(
                            requestWithToken.newBuilder()
                                .header("Authorization", "Bearer ${authData.token}")
                                .build()
                        )
                    }
                }
            }
        }

        return response
    }

    private fun refreshAccessToken(refreshToken: String): AuthResponse.AuthData? {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        
        val okHttpClient = okhttp3.OkHttpClient.Builder().build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(TEMP_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val authApi = retrofit.create(AuthApi::class.java)

        return try {
            val response = authApi.refresh(RefreshRequest(refreshToken)).execute()
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing token")
            null
        }
    }
}
