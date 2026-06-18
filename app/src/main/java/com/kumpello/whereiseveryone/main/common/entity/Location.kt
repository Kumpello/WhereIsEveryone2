package com.kumpello.whereiseveryone.main.common.entity

import androidx.compose.runtime.Immutable

@Immutable
data class Location(
    val lat: Double,
    val lon: Double,
    val bearing: Float?,
    val alt: AltDifference,
    val rawAlt: Double?,
    val accuracy: AccuracyLevel,
    val rawAccuracy: Float?,
    val lastUpdateTime: String,
    val lastUpdateAge: LastUpdateAge,
)