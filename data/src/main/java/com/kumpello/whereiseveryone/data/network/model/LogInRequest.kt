package com.kumpello.whereiseveryone.data.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LogInRequest(
    val username: String,
    val password: String,
    val device_token: String? = null
)
