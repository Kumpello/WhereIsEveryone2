package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.SharingRepository

class GetPausedFriendsUseCase(
    private val sharingRepository: SharingRepository,
    private val getCurrentAuthTokenUseCase: GetCurrentAuthTokenUseCase
) {
    suspend fun execute(): SharingResponse {
        return sharingRepository.getPausedFriends(
            token = getCurrentAuthTokenUseCase.execute().toString()
        )
    }
}
