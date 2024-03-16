package com.kumpello.whereiseveryone.common.domain.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private const val tempBaseUrl = "http://192.168.0.216:8080/api/"

    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(RequestInterceptor)
        .build()

    fun getClient(): Retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(tempBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
}