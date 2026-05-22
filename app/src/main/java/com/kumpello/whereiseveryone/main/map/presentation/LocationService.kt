package com.kumpello.whereiseveryone.main.map.presentation

import android.location.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LocationService {
    fun changeForegroundUpdateInterval(interval: Long)
    fun changeBackgroundUpdateInterval(interval: Long)

    fun stopLocationService()

    fun observeLocation() : StateFlow<Location>

    fun changeUpdateType(updateType: UpdateType)

    sealed interface UpdateType {
        data object Foreground : UpdateType
        data object Background : UpdateType
    }

}