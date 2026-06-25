package com.kumpello.whereiseveryone.main.common.entity

data class FriendLocalData(
    val username: String,
    val status: String,
    val state: FriendState,
    val location: LocationData?
)
