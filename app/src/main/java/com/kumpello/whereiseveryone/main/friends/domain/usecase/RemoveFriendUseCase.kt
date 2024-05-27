package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.main.friends.domain.repository.ObserveRepository

class RemoveFriendUseCase(
    private val observeRepository: ObserveRepository,
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
) {
    fun execute(nick: String): CodeResponse {
        return observeRepository.removeFriend(
            token = getCurrentAuthKeyUseCase.execute().toString(),
            nick = nick
        )
    }
}