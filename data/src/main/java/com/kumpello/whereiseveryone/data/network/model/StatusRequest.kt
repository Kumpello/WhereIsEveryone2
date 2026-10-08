package com.kumpello.whereiseveryone.data.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatusRequest(
    val status: String
)