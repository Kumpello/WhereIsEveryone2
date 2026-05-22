package com.kumpello.whereiseveryone.main.friends.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FriendRequest(
    val username: String
)