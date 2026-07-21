package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepository

class SendLocationUseCase(
    private val locationRepository: LocationRepository,
    private val preferencesManager: PreferencesManager
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
            token = preferencesManager.get(PreferencesKey.AuthToken).toString(),
            longitude = longitude,
            latitude = latitude,
            bearing = bearing,
            altitude = altitude,
            accuracy = accuracy,
            lastUpdate = lastUpdate
        )
    }
}