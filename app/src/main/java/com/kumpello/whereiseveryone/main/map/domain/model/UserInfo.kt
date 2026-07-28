package com.kumpello.whereiseveryone.main.map.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserInfo(
    val latitude: Double,
    val longitude: Double,
    val bearing: Float?,
    val altitude: Double?,
    val accuracy: Float?,
    val speed: Float?,
    val last_update: Long
)