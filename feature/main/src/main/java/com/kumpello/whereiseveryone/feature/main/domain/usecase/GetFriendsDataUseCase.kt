package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.repository.FriendsRepository
import com.kumpello.whereiseveryone.data.model.FriendsResponse

class GetFriendsDataUseCase(
    private val friendsRepository: FriendsRepository
) {
    suspend fun execute(): FriendsResponse {
        return friendsRepository.getFriends()
    }
}
