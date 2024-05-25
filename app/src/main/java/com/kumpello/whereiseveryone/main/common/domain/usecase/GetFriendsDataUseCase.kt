package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.main.common.domain.repository.FriendsRepository
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse

class GetFriendsDataUseCase(
    private val friendsRepository: FriendsRepository,
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
) {
    fun execute(): PositionsResponse {
        return friendsRepository.getPositions(
            token = getCurrentAuthKeyUseCase.execute().toString()
        )
    }
}