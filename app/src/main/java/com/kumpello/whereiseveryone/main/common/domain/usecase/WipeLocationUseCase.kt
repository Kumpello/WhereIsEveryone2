package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepository

class WipeLocationUseCase(
    private val locationRepository: LocationRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend fun execute(): CodeResponse {
        return locationRepository.wipeLocation(
            token = preferencesManager.get(PreferencesKey.AuthToken).toString()
        )
    }
}