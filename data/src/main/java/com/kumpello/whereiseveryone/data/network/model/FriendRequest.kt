package com.kumpello.whereiseveryone.data.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FriendRequest(
    val username: String
)