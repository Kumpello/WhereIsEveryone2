package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.data.model.FriendsResponse

class GetFriendsDataUseCase(
    private val friendsRepository: FriendsStateRepository
) {
    suspend fun execute(): FriendsResponse {
        return friendsRepository.refresh()
    }
}
