package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse

sealed interface FriendRepository {
    suspend fun addFriend(username: String): CodeResponse

    suspend fun removeFriend(username: String): CodeResponse

    suspend fun acceptFriendRequest(username: String): CodeResponse

    suspend fun rejectFriendRequest(username: String): CodeResponse
}