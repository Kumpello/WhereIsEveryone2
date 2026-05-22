package com.kumpello.whereiseveryone.main.map.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatusRequest(
    val status: String
)