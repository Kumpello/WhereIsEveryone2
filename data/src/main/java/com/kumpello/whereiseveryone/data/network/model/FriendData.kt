package com.kumpello.whereiseveryone.data.network.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class FriendData(
    val username: String,
    val status: String = "",
    val state: String,
    val location: UserInfo?,
    val friend_since: Long?,
    @Json(name = "points_of_interest") val pointsOfInterest: List<PointOfInterestData> = emptyList(),
)
