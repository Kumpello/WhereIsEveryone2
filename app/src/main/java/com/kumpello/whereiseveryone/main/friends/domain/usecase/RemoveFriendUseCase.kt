package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository

class RemoveFriendUseCase(
    private val friendRepository: FriendRepository
) {
    suspend fun execute(username: String): CodeResponse {
        return friendRepository.removeFriend(
            username = username
        )
    }
}