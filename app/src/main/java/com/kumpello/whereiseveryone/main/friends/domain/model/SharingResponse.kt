package com.kumpello.whereiseveryone.main.friends.domain.model

import com.squareup.moshi.JsonClass

sealed interface SharingResponse {
    @JsonClass(generateAdapter = true)
    data class PausedFriends(val usernames: List<String>): SharingResponse

    @JsonClass(generateAdapter = true)
    data class ErrorData(val code: Int, val error: String, val message: String): SharingResponse
}
