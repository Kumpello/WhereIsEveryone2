package com.kumpello.whereiseveryone.feature.main.location

import com.kumpello.whereiseveryone.data.model.LocationData

internal fun resolveLocationUpdateType(
    appForeground: Boolean,
    forcedForeground: Boolean,
    isCharging: Boolean,
    isCarMode: Boolean,
): LocationService.UpdateType = when {
    isCharging -> LocationService.UpdateType.Charging
    isCarMode -> LocationService.UpdateType.CarMode
    appForeground || forcedForeground -> LocationService.UpdateType.Foreground
    else -> LocationService.UpdateType.Background
}

internal fun shouldAcquireLocations(sharingEnabled: Boolean, appForeground: Boolean) =
    sharingEnabled || appForeground

internal fun LocationService.UpdateType.uploadIntervalMillis(): Long = when (this) {
    LocationService.UpdateType.Charging, LocationService.UpdateType.CarMode -> 1_000L
    LocationService.UpdateType.Foreground, LocationService.UpdateType.Background -> 5_000L
}

internal fun mapDisplayFix(
    raw: LocationData?,
    display: LocationData?,
    status: LocationService.Status,
    now: Long,
): LocationData? = if (raw != null && display != null && status.isCarMode && status.isAppForeground &&
    now - display.lastUpdate in 0..10_000L && raw.lastUpdate - display.lastUpdate in 0..1_000L &&
    display.lat.isFinite() && display.lat in -90.0..90.0 && display.lon.isFinite() && display.lon in -180.0..180.0) {
    raw.copy(lat = display.lat, lon = display.lon)
} else raw
