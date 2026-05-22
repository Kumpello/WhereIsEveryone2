package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse

sealed interface FriendRepository {
    fun addFriend(token: String, username: String): CodeResponse

    fun removeFriend(token: String, username: String): CodeResponse

    fun acceptFriendRequest(token: String, username: String): CodeResponse

    fun rejectFriendRequest(token: String, username: String): CodeResponse
}