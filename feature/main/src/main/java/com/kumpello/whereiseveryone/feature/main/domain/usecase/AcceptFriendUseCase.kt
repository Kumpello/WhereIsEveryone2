package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.FriendRepository
import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository

class AcceptFriendUseCase(
    private val friendRepository: FriendRepository,
    private val friendsStateRepository: FriendsStateRepository,
) {
    suspend fun execute(username: String): CodeResponse {
        val response = friendRepository.acceptFriendRequest(
            username = username
        )
        if (response == CodeResponse.SuccessNoContent) {
            friendsStateRepository.resolveIncomingFriendRequest(username)
        }
        return response
    }
}
