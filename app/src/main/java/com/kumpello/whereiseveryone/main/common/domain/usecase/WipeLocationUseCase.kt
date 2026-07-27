package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepository

class WipeLocationUseCase(
    private val locationRepository: LocationRepository
) {
    suspend fun execute(): CodeResponse {
        return locationRepository.wipeLocation()
    }
}