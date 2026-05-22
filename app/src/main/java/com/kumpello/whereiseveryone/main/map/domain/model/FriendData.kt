package com.kumpello.whereiseveryone.main.map.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FriendData(
    val username: String,
    val status: String,
    val state: String,
    val location: UserInfo
)
