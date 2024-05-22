package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse

interface FriendsRepository {
    fun getPositions(token: String): PositionsResponse
}