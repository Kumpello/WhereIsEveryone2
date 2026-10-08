package com.kumpello.whereiseveryone.data.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FriendsPage(
    val items: List<FriendData>,
    val next_cursor: String?
)
