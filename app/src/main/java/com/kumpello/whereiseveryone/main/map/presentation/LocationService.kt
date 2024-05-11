package com.kumpello.whereiseveryone.main.map.presentation

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationService {
    fun changeForegroundUpdateInterval(interval: Long)
    fun changeBackgroundUpdateInterval(interval: Long)

    fun getLocation() : Flow<Location>

    fun changeUpdateType(updateType: UpdateType)

    sealed interface UpdateType {
        data object Foreground : UpdateType
        data object Background : UpdateType
    }

}