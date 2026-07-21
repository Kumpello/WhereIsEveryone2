package com.kumpello.whereiseveryone.main.common.entity

data class LocationData(
    val lat: Double,
    val lon: Double,
    val bearing: Float?,
    val alt: Double?,
    val accuracy: Float?,
    val lastUpdate: Long
)
