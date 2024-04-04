package com.kumpello.whereiseveryone.main.map.presentation

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationService {
    fun changeForegroundUpdateInterval(interval: Int)
    fun changeBackgroundUpdateInterval(interval: Int)
    fun setUpdateInterval(type: UpdateInterval)

    fun getLocation() : Flow<Location>

    sealed interface UpdateInterval {
        data object Foreground : UpdateInterval
        data object Background : UpdateInterval
    }
}