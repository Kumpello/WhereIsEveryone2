package com.kumpello.whereiseveryone.data.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshRequest(
    val refresh_token: String,
    val device_token: String? = null
)
