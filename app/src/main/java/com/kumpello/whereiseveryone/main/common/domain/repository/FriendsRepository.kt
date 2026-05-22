package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse

interface FriendsRepository {
    fun getFriends(token: String): FriendsResponse
}