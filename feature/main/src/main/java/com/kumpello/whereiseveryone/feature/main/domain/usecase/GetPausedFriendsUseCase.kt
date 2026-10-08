package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.network.model.SharingResponse
import com.kumpello.whereiseveryone.data.repository.SharingRepository

class GetPausedFriendsUseCase(
    private val sharingRepository: SharingRepository
) {
    suspend fun execute(): SharingResponse {
        return sharingRepository.getPausedFriends()
    }
}
