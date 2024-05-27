package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse

sealed interface ObserveRepository {
    fun addFriend(token: String, nick: String): CodeResponse

    fun removeFriend(token: String, nick: String): CodeResponse
}