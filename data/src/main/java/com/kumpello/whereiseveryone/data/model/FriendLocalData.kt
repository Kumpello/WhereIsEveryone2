package com.kumpello.whereiseveryone.data.model

import com.kumpello.whereiseveryone.data.network.model.FriendData

data class FriendLocalData(
    val username: String,
    val status: String,
    val state: FriendState,
    val location: LocationData?,
    val friendSince: Long?
)

fun FriendData.toLocalData() = FriendLocalData(
    username = username,
    status = status,
    state = state.toFriendState(),
    location = location?.let { loc ->
        LocationData(
            lat = loc.latitude,
            lon = loc.longitude,
            bearing = loc.bearing,
            alt = loc.altitude,
            accuracy = loc.accuracy,
            speed = loc.speed,
            lastUpdate = loc.last_update
        )
    },
    friendSince = friend_since
)
