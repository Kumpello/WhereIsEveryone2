package com.kumpello.whereiseveryone.main.friends.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserInfo(val lat: Double, val lon: Double, val bearing: Float, val alt: Double, val accuracy: Float, val message: String)
