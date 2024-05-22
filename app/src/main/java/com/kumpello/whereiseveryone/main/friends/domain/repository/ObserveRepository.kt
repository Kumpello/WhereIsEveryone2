package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.model.ErrorData

sealed interface ObserveRepository {
    fun addFriend(token: String, nick: String): ErrorData?

    fun removeFriend(token: String, nick: String): ErrorData?
}