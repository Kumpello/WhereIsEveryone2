package com.kumpello.whereiseveryone.main.map.data.model

import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.kumpello.whereiseveryone.main.friends.model.UserInfo
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FriendData(val last_update: String, val friend: Friend, val location: UserInfo)
