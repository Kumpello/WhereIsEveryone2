package com.kumpello.whereiseveryone.data.restore

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface RestoreCredentialsApi {
    @POST("auth/restore/register/options")
    suspend fun registrationOptions(
        @Header("Authorization") authorization: String,
        @Body request: RestoreOptionsRequest
    ): RestoreOptionsResponse

    @POST("auth/restore/register/finish")
    suspend fun register(
        @Header("Authorization") authorization: String,
        @Body request: RestoreFinishRequest
    )

    @POST("auth/restore/login/options")
    suspend fun authenticationOptions(@Body request: RestoreOptionsRequest): RestoreOptionsResponse

    @POST("auth/restore/login/finish")
    suspend fun authenticate(@Body request: RestoreFinishRequest): RestoredSession
}

@JsonClass(generateAdapter = true)
internal data class RestoreOptionsRequest(val app_id: String, val device_token: String)

@JsonClass(generateAdapter = true)
internal data class RestoreOptionsResponse(val challenge_id: String, val request_json: String)

@JsonClass(generateAdapter = true)
internal data class RestoreFinishRequest(
    val app_id: String,
    val device_token: String,
    val challenge_id: String,
    val credential_json: String
)

@JsonClass(generateAdapter = true)
internal data class RestoredSession(
    val id: String,
    val username: String,
    val token: String,
    val refresh_token: String
)
