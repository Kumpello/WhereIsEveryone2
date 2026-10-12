package com.kumpello.whereiseveryone.feature.main.location

import android.location.Location
import com.kumpello.whereiseveryone.data.model.LocationData
import kotlinx.coroutines.flow.StateFlow

interface LocationService {
    fun startLocationService()
    fun stopLocationService()
    fun toggleSharing()
    fun observeIsServiceRunning(): StateFlow<Boolean>

    fun changeForegroundUpdateInterval(interval: Long)
    fun changeBackgroundUpdateInterval(interval: Long)

    fun observeLocation() : StateFlow<Location?>

    fun setAppForeground(isForeground: Boolean)
    fun observeStatus(): StateFlow<Status>

    /** A display-only correction for the current fix; never changes acquired or uploaded data. */
    fun setMapDisplayLocation(location: LocationData?)

    fun setForcedForeground(durationSeconds: Long?)
    fun disableForcedForeground()
    fun observeForcedForegroundStatus(): StateFlow<ForcedForegroundStatus>

    data class ForcedForegroundStatus(
        val isEnabled: Boolean = false,
        val endTime: Long? = null
    )

    data class Status(
        val updateType: UpdateType = UpdateType.Foreground,
        val isAppForeground: Boolean = true,
        val isCarMode: Boolean = false,
        val isCharging: Boolean = false,
        val isSharingEnabled: Boolean = false,
    )

    sealed interface UpdateType {
        data object Charging : UpdateType
        data object CarMode : UpdateType
        data object Foreground : UpdateType
        data object Background : UpdateType
    }
}
