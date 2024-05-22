package com.kumpello.whereiseveryone.main.map.data.model

import com.squareup.moshi.JsonClass

interface PositionsResponse {
    @JsonClass(generateAdapter = true)
    data class PositionsData(val positions: List<FriendPosition>?)
}
