package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse

sealed interface LocationRepository {
    suspend fun sendPosition(
        longitude: Double,
        latitude: Double,
        bearing: Float,
        altitude: Double,
        accuracy: Float,
        speed: Float,
        lastUpdate: Long
    ): CodeResponse

    suspend fun wipeLocation(): CodeResponse
}