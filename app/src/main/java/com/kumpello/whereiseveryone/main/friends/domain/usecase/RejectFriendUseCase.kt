package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository

class RejectFriendUseCase(
    private val friendRepository: FriendRepository,
    private val getCurrentAuthTokenUseCase: GetCurrentAuthTokenUseCase
) {
    fun execute(username: String): CodeResponse {
        return friendRepository.rejectFriendRequest(
            token = getCurrentAuthTokenUseCase.execute().toString(),
            username = username
        )
    }
}