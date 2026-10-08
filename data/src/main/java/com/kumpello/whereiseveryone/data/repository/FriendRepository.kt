package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.CodeResponse

sealed interface FriendRepository {
    suspend fun addFriend(username: String): CodeResponse

    suspend fun removeFriend(username: String): CodeResponse

    suspend fun acceptFriendRequest(username: String): CodeResponse

    suspend fun rejectFriendRequest(username: String): CodeResponse
}