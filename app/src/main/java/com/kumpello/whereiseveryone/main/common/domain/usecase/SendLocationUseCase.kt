package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepository

class SendLocationUseCase(
    private val locationRepository: LocationRepository
) {
    suspend fun execute(
        longitude: Double,
        latitude: Double,
        bearing: Float,
        altitude: Double,
        accuracy: Float,
        lastUpdate: Long
    ): CodeResponse {
        return locationRepository.sendPosition(
            longitude = longitude,
            latitude = latitude,
            bearing = bearing,
            altitude = altitude,
            accuracy = accuracy,
            lastUpdate = lastUpdate
        )
    }
}