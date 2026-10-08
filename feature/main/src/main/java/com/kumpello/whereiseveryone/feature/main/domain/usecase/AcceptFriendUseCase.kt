package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.FriendRepository

class AcceptFriendUseCase(
    private val friendRepository: FriendRepository
) {
    suspend fun execute(username: String): CodeResponse {
        return friendRepository.acceptFriendRequest(
            username = username
        )
    }
}