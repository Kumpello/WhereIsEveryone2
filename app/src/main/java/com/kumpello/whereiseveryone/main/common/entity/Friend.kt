package com.kumpello.whereiseveryone.main.common.entity

import androidx.compose.runtime.Immutable

@Immutable
data class Friend(
    val username: String,
    val status: String,
    val state: FriendState,
    val location: Location,
    val distance: Double? = null,
    val formattedDistance: String? = null
)
