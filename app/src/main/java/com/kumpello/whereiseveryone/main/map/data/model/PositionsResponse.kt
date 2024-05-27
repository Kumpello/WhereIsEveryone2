package com.kumpello.whereiseveryone.main.map.data.model

import com.squareup.moshi.JsonClass

sealed interface PositionsResponse {
    @JsonClass(generateAdapter = true)
    data class FriendsData(val positions: List<FriendData>?): PositionsResponse

    @JsonClass(generateAdapter = true)
    data class ErrorData(val code : Int, val error : String, val message : String): PositionsResponse
}
