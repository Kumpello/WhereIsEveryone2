package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.main.common.domain.repository.FriendsRepository
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse

class GetFriendsDataUseCase(
    private val friendsRepository: FriendsRepository,
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
) {
    fun execute(): FriendsResponse {
        return friendsRepository.getFriends(
            token = getCurrentAuthKeyUseCase.execute().toString()
        )
    }
}