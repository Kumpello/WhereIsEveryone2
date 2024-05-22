package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.main.map.data.model.LocationResponse

sealed interface LocationRepository {
    fun sendPosition(token: String, longitude: Double, latitude: Double): LocationResponse
}