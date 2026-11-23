package com.kumpello.whereiseveryone.data.diagnostics

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface TelemetryApi {
    @POST("me/telemetry")
    suspend fun upload(
        @Header("Authorization") authorization: String,
        @Body batch: RequestBody,
    ): Response<Unit>
}
