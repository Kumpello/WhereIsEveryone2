package com.kumpello.whereiseveryone.main.map.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocationRequest(val longitude: Double, val latitude: Double)
