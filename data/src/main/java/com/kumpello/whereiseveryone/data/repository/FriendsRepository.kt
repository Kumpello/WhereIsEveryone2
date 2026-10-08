package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.FriendsResponse

interface FriendsRepository {
    suspend fun getFriends(): FriendsResponse
}