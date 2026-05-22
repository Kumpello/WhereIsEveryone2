package com.kumpello.whereiseveryone.main.common.entity

data class Friend(
    val username: String,
    val status: String,
    val state: FriendState,
    val location: Location
)
