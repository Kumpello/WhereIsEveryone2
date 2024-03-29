package com.kumpello.whereiseveryone.main.friends.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FriendList(@Json(name = "list") val list: List<Friend>)