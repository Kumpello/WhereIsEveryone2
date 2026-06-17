package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository

class AcceptFriendUseCase(
    private val friendRepository: FriendRepository,
    private val getCurrentAuthTokenUseCase: GetCurrentAuthTokenUseCase
) {
    suspend fun execute(username: String): CodeResponse {
        return friendRepository.acceptFriendRequest(
            token = getCurrentAuthTokenUseCase.execute().toString(),
            username = username
        )
    }
}