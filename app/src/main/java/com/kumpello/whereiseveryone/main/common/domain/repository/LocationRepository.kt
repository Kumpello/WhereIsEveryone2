package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import kotlin.time.Instant

sealed interface LocationRepository {
    fun sendPosition(
        token: String,
        longitude: Double,
        latitude: Double,
        bearing: Float,
        altitude: Double,
        accuracy: Float,
        lastUpdate: Instant
    ): CodeResponse
}