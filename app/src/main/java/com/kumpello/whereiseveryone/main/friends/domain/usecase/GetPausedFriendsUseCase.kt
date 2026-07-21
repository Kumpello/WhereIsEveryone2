package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.SharingRepository

class GetPausedFriendsUseCase(
    private val sharingRepository: SharingRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend fun execute(): SharingResponse {
        return sharingRepository.getPausedFriends(
            token = preferencesManager.get(PreferencesKey.AuthToken).toString()
        )
    }
}
