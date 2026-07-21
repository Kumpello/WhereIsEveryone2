package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository

class AcceptFriendUseCase(
    private val friendRepository: FriendRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend fun execute(username: String): CodeResponse {
        return friendRepository.acceptFriendRequest(
            token = preferencesManager.get(PreferencesKey.AuthToken).toString(),
            username = username
        )
    }
}