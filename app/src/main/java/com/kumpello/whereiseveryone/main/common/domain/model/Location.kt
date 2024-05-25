package com.kumpello.whereiseveryone.main.common.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Location(val lat: Double, val lon: Double, val bearing: Float, val alt: Double, val accuracy: Float)
