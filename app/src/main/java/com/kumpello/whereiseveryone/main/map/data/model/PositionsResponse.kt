package com.kumpello.whereiseveryone.main.map.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PositionsResponse(val positions: List<FriendPosition>?)
