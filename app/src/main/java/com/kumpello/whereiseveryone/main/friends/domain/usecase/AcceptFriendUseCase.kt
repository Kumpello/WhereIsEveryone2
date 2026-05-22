package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository

class AcceptFriendUseCase(
    private val friendRepository: FriendRepository,
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
) {
    fun execute(username: String): CodeResponse {
        return friendRepository.acceptFriendRequest(
            token = getCurrentAuthKeyUseCase.execute().toString(),
            username = username
        )
    }
}