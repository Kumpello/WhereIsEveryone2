package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.SharingRepository

class StopSharingUseCase(
    private val sharingRepository: SharingRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend fun execute(username: String): CodeResponse {
        return sharingRepository.stopSharing(
            token = preferencesManager.get(PreferencesKey.AuthToken).toString(),
            username = username
        )
    }
}
