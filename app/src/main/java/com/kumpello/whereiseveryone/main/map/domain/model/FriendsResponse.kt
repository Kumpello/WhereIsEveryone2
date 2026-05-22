package com.kumpello.whereiseveryone.main.map.domain.model

import com.squareup.moshi.JsonClass

sealed interface FriendsResponse {
    @JsonClass(generateAdapter = true)
    data class FriendsData(val positions: List<FriendData>): FriendsResponse

    @JsonClass(generateAdapter = true)
    data class ErrorData(val code : Int, val error : String, val message : String): FriendsResponse
}
