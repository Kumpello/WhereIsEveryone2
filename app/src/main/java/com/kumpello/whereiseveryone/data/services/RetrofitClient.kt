package com.kumpello.whereiseveryone.data.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val tempBaseUrl = "http://192.168.0.146:8080"

    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(RequestInterceptor)
        .build()

    fun getClient(): Retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(tempBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}