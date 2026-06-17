package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse

sealed interface FriendRepository {
    suspend fun addFriend(token: String, username: String): CodeResponse

    suspend fun removeFriend(token: String, username: String): CodeResponse

    suspend fun acceptFriendRequest(token: String, username: String): CodeResponse

    suspend fun rejectFriendRequest(token: String, username: String): CodeResponse
}