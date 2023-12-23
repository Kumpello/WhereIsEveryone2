package com.kumpello.whereiseveryone.main.map.domain.usecase

import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.main.map.data.model.LocationResponse
import com.kumpello.whereiseveryone.main.map.domain.repository.LocationRepository

class SendPositionUseCase(
    private val locationRepository: LocationRepository,
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
) {
    fun execute(longitude: Double, latitude: Double): LocationResponse {
        return locationRepository.sendPosition(
            token = getCurrentAuthKeyUseCase.execute(),
            longitude = longitude,
            latitude = latitude
        )
    }
}