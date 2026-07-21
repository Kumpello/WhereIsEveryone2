package com.kumpello.whereiseveryone.main.map.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocationRequest(
    val longitude: Double,
    val latitude: Double,
    val bearing: Float?,
    val altitude: Double?,
    val accuracy: Float?,
    val last_update: Long
)
