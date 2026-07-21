package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository

class RemoveFriendUseCase(
    private val friendRepository: FriendRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend fun execute(username: String): CodeResponse {
        return friendRepository.removeFriend(
            token = preferencesManager.get(PreferencesKey.AuthToken).toString(),
            username = username
        )
    }
}