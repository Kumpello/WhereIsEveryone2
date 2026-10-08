package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.LocationRepository

class SendLocationUseCase(
    private val locationRepository: LocationRepository
) {
    suspend fun execute(
        longitude: Double,
        latitude: Double,
        bearing: Float,
        altitude: Double,
        accuracy: Float,
        speed: Float,
        lastUpdate: Long
    ): CodeResponse {
        return locationRepository.sendPosition(
            longitude = longitude,
            latitude = latitude,
            bearing = bearing,
            altitude = altitude,
            accuracy = accuracy,
            speed = speed,
            lastUpdate = lastUpdate
        )
    }
}