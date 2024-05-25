package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.SuccessResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepository

class SendLocationUseCase(
    private val locationRepository: LocationRepository,
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
) {
    fun execute(
        longitude: Double,
        latitude: Double,
        bearing: Float,
        altitude: Double,
        accuracy: Float
    ): SuccessResponse {
        return locationRepository.sendPosition(
            token = getCurrentAuthKeyUseCase.execute().toString(),
            longitude = longitude,
            latitude = latitude,
            bearing = bearing,
            altitude = altitude,
            accuracy = accuracy
        )
    }
}