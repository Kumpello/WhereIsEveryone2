package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.main.map.data.model.LocationResponse
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse

interface LocationRepository {
    fun sendPosition(token: String, longitude: Double, latitude: Double): LocationResponse
    fun getPositions(token: String, users: List<String>, uuids: List<String>): PositionsResponse
}