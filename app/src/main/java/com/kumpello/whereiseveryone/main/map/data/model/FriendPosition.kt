package com.kumpello.whereiseveryone.main.map.data.model

import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.kumpello.whereiseveryone.main.friends.model.Location
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FriendPosition(val friend: Friend, val location: Location)
