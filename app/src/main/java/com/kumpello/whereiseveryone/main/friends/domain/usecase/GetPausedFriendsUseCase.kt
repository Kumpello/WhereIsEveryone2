package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.SharingRepository

class GetPausedFriendsUseCase(
    private val sharingRepository: SharingRepository
) {
    suspend fun execute(): SharingResponse {
        return sharingRepository.getPausedFriends()
    }
}
