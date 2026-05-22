package com.kumpello.whereiseveryone.main.common.entity

enum class FriendState(val state: String) {
    ACCEPTED("accepted"),
    PENDING_INCOMING("pending_incoming"),
    PENDING_OUTGOING("pending_outgoing"),
}

fun String.toFriendState(): FriendState = when {
    equals("accepted") -> FriendState.ACCEPTED
    equals("pending_incoming") -> FriendState.PENDING_INCOMING
    equals("pending_outgoing") -> FriendState.PENDING_OUTGOING
    else -> throw IllegalArgumentException("Unknown FriendState: $this")
}