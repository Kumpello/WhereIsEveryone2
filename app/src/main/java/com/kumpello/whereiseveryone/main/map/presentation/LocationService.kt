package com.kumpello.whereiseveryone.main.map.presentation

import android.location.Location
import kotlinx.coroutines.flow.StateFlow

interface LocationService {
    fun startLocationService()
    fun stopLocationService()
    fun toggleSharing()
    fun observeIsServiceRunning(): StateFlow<Boolean>

    fun changeForegroundUpdateInterval(interval: Long)
    fun changeBackgroundUpdateInterval(interval: Long)

    fun observeLocation() : StateFlow<Location?>

    fun changeUpdateType(updateType: UpdateType)

    fun setForcedForeground(durationSeconds: Long?)
    fun disableForcedForeground()
    fun observeForcedForegroundStatus(): StateFlow<ForcedForegroundStatus>

    data class ForcedForegroundStatus(
        val isEnabled: Boolean = false,
        val endTime: Long? = null
    )

    sealed interface UpdateType {
        data object Foreground : UpdateType
        data object Background : UpdateType
    }
}
