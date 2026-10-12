package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.CodeResponse

sealed interface LocationRepository {
    suspend fun sendPosition(
        longitude: Double,
        latitude: Double,
        bearing: Float?,
        altitude: Double,
        accuracy: Float,
        speed: Float?,
        lastUpdate: Long,
        isCarMode: Boolean = false,
    ): CodeResponse

    suspend fun wipeLocation(): CodeResponse
}
