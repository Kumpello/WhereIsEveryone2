package com.kumpello.whereiseveryone.main.map.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocationResponse(val statusCode : Int)