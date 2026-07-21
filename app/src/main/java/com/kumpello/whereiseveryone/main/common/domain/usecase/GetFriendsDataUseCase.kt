package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.main.common.domain.repository.FriendsRepository
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse

class GetFriendsDataUseCase(
    private val friendsRepository: FriendsRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend fun execute(): FriendsResponse {
        return friendsRepository.getFriends(
            token = preferencesManager.get(PreferencesKey.AuthToken).toString()
        )
    }
}
