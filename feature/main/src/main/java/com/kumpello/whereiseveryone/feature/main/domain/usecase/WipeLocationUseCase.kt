package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.LocationRepository

class WipeLocationUseCase(
    private val locationRepository: LocationRepository
) {
    suspend fun execute(): CodeResponse {
        return locationRepository.wipeLocation()
    }
}