package com.kumpello.whereiseveryone.common.domain.services

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.time.Instant

object RetrofitClient {
    private const val TEMP_BASE_URL = "http://192.168.1.216:8080/api/"

    private val moshi = Moshi.Builder()
        .add(InstantAdapter)
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(RequestInterceptor)
        .build()

    fun getClient(): Retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(TEMP_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
}

object InstantAdapter {
    @ToJson
    fun toJson(value: Instant): String {
        return value.toString()
    }

    @FromJson
    fun fromJson(value: String): Instant {
        return Instant.parse(value)
    }
}