package com.kumpello.whereiseveryone.main.map.presentation

import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import kotlinx.coroutines.flow.SharedFlow

interface PositionsService {
    fun startFriendsUpdates()
    fun getFriendsFlow(): SharedFlow<PositionsResponse>
}