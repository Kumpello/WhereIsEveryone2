package com.kumpello.whereiseveryone.main.friends.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Location(val lat: Double, val lon: Double, val bearing: Float, val alt: Double)
