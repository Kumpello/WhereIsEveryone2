package com.kumpello.whereiseveryone.main.common.entity

import kotlin.time.Instant

data class FriendLocalData(
    val username: String,
    val status: String,
    val state: FriendState,
    val location: LocationData?,
    val friendSince: Instant?
)

