package com.kumpello.whereiseveryone.main.common.entity

import kotlin.time.Instant

data class LocationData(
    val lat: Double,
    val lon: Double,
    val bearing: Float?,
    val alt: Double?,
    val accuracy: Float?,
    val last_update: Instant
)
