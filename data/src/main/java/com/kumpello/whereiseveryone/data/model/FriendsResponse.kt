package com.kumpello.whereiseveryone.data.model

import com.kumpello.whereiseveryone.data.network.model.FriendData
import com.squareup.moshi.JsonClass

sealed interface FriendsResponse {
    @JsonClass(generateAdapter = true)
    data class FriendsData(val positions: List<FriendData>): FriendsResponse

    @JsonClass(generateAdapter = true)
    data class ErrorData(val code : Int, val error : String, val message : String): FriendsResponse
}
