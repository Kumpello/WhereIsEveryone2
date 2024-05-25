package com.kumpello.whereiseveryone.main.map.data.model

import com.squareup.moshi.JsonClass

interface PositionsResponse {
    @JsonClass(generateAdapter = true)
    data class FriendsData(val positions: List<FriendData>?): PositionsResponse
}
