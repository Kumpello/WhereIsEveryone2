package com.kumpello.whereiseveryone.data.network.model

import com.kumpello.whereiseveryone.data.model.PointOfInterest
import com.kumpello.whereiseveryone.data.model.isValidPointText
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

fun PointOfInterestData.toPointOfInterest(isOwn: Boolean = false): PointOfInterest {
    require(id.isNotBlank() && creatorUsername.isNotBlank() && createdAt > 0)
    require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
    require(isValidPointText(name, description))
    return PointOfInterest(id, name, description, latitude, longitude, creatorUsername, createdAt, isOwn)
}

@JsonClass(generateAdapter = true)
data class PointOfInterestData(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    @Json(name = "creator_username") val creatorUsername: String,
    @Json(name = "created_at") val createdAt: Long,
)

@JsonClass(generateAdapter = true)
data class PointOfInterestRequest(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
)
