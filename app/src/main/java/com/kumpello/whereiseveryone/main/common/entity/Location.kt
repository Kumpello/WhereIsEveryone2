package com.kumpello.whereiseveryone.main.common.entity

data class Location(
    val lat: Double,
    val lon: Double,
    val bearing: Float?,
    val alt: AltDifference,
    val accuracy: AccuracyLevel,
    val lastUpdateTime: String,
    val lastUpdateAge: LastUpdateAge,
)