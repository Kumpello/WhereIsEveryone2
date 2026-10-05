package com.kumpello.whereiseveryone.main.map.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FriendsPage(
    val items: List<FriendData>,
    val next_cursor: String?
)
