package com.kumpello.whereiseveryone.main.map.domain.usecase

import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import com.kumpello.whereiseveryone.main.map.domain.repository.LocationRepository

class GetFriendsPositionsUseCase(
    private val locationRepository: LocationRepository,
    private val getKeyUseCase: GetKeyUseCase,
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
) {
    fun execute(): PositionsResponse {
        val friends = getKeyUseCase.getFriends()
            .map { Pair(it.nick, it.id) }.unzip()
        return locationRepository.getPositions(
            token = getCurrentAuthKeyUseCase.execute(),
            users = friends.first,
            uuids = friends.second
        )
    }
}