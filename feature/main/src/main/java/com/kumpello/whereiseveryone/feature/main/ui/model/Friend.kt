package com.kumpello.whereiseveryone.feature.main.ui.model

import com.kumpello.whereiseveryone.data.model.FriendState
import androidx.compose.runtime.Immutable

@Immutable
data class Friend(
    val username: String,
    val status: String,
    val state: FriendState,
    val location: Location?,
    val distance: Double? = null,
    val formattedDistance: String? = null,
    val isPaused: Boolean = false,
    val friendSince: String? = null
)
