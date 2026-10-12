package com.kumpello.whereiseveryone.data.network.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class UserInfo(
    val latitude: Double,
    val longitude: Double,
    val bearing: Float?,
    val altitude: Double?,
    val accuracy: Float?,
    val speed: Float?,
    val last_update: Long,
    @Json(name = "car_mode") val isCarMode: Boolean = false,
)
