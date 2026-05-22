package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository

class RemoveFriendUseCase(
    private val friendRepository: FriendRepository,
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
) {
    fun execute(username: String): CodeResponse {
        return friendRepository.removeFriend(
            token = getCurrentAuthKeyUseCase.execute().toString(),
            username = username
        )
    }
}