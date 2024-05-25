package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.SuccessResponse

sealed interface ObserveRepository {
    fun addFriend(token: String, nick: String): SuccessResponse

    fun removeFriend(token: String, nick: String): SuccessResponse
}