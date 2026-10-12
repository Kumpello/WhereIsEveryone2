package com.kumpello.whereiseveryone.feature.main.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.kumpello.whereiseveryone.data.model.LocationData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

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

internal fun isBatteryCharging(status: Int, plugged: Int) = plugged != 0 &&
    (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)

internal fun Context.chargingStatus(): Flow<Boolean> = callbackFlow {
    fun publish(intent: Intent?) {
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        trySend(isBatteryCharging(status, plugged))
    }
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = publish(intent)
    }
    publish(ContextCompat.registerReceiver(
        this@chargingStatus,
        receiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    ))
    awaitClose { unregisterReceiver(receiver) }
}.distinctUntilChanged().conflate()
