package com.kumpello.whereiseveryone.common.domain.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private const val TEMP_BASE_URL = "http://192.168.0.146:8080/api/"

    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(RequestInterceptor)
        .build()

    fun getClient(): Retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(TEMP_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
}