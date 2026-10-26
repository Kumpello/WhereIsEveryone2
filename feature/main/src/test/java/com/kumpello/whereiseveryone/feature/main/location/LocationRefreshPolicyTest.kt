package com.kumpello.whereiseveryone.feature.main.location

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import com.kumpello.whereiseveryone.data.model.LocationData

class LocationRefreshPolicyTest {
    @Test
    fun `paused sharing serves the visible map locally and stops GPS in background`() {
        assertTrue(shouldAcquireLocations(false, true))
        assertFalse(shouldAcquireLocations(false, false))
        assertTrue(shouldAcquireLocations(true, false))
        assertTrue(shouldAcquireLocations(true, true))
    }
    @Test
    fun `charging takes priority over visibility and forced foreground`() {
        for (foreground in listOf(false, true)) for (forced in listOf(false, true)) for (car in listOf(false, true)) {
            assertEquals(LocationService.UpdateType.Charging, resolveLocationUpdateType(foreground, forced, isCharging = true, isCarMode = car))
        }
    }

    @Test
    fun `unplugging uses current visibility and respects forced foreground`() {
        assertEquals(LocationService.UpdateType.Background, resolveLocationUpdateType(false, false, false, false))
        assertEquals(LocationService.UpdateType.Foreground, resolveLocationUpdateType(true, false, false, false))
        assertEquals(LocationService.UpdateType.Foreground, resolveLocationUpdateType(false, true, false, false))
        assertEquals(LocationService.UpdateType.CarMode, resolveLocationUpdateType(false, false, false, true))
        assertEquals(LocationService.UpdateType.CarMode, resolveLocationUpdateType(false, true, false, true))
    }

    @Test
    fun `car mode is an acquisition state even when the Activity is stopped`() {
        assertEquals(LocationService.UpdateType.CarMode, resolveLocationUpdateType(false, false, false, true))
        assertEquals(LocationService.UpdateType.CarMode, resolveLocationUpdateType(true, false, false, true))
        assertEquals(1_000L, LocationService.UpdateType.CarMode.uploadIntervalMillis())
        assertEquals(1_000L, LocationService.UpdateType.Charging.uploadIntervalMillis())
        assertEquals(5_000L, LocationService.UpdateType.Foreground.uploadIntervalMillis())
        assertEquals(5_000L, LocationService.UpdateType.Background.uploadIntervalMillis())
    }

    @Test
    fun `display correction preserves GPS vector and never replaces the acquired fix`() {
        val now = 1_800_000_000_000L
        val raw = LocationData(52.0, 21.0001, 90f, null, 10f, 20f, now, true)
        val snapped = raw.copy(lon = 21.0)
        val status = LocationService.Status(isCarMode = true)
        assertEquals(snapped, mapDisplayFix(raw, snapped, status, now))
        assertEquals(90f, raw.bearing)
        assertEquals(21.0001, raw.lon, 0.0)
        assertEquals(90f, mapDisplayFix(raw, snapped.copy(bearing = 180f, speed = 0f), status, now)!!.bearing)
        assertEquals(20f, mapDisplayFix(raw, snapped.copy(bearing = 180f, speed = 0f), status, now)!!.speed)
        assertEquals(raw, mapDisplayFix(raw, snapped, status.copy(isCarMode = false), now))
        assertEquals(raw, mapDisplayFix(raw, snapped, status.copy(isAppForeground = false), now))
        assertEquals(raw, mapDisplayFix(raw, snapped, status, now + 10_001))
        assertEquals(raw, mapDisplayFix(raw, snapped.copy(lastUpdate = now - 1_001), status, now))
        assertEquals(raw, mapDisplayFix(raw, snapped.copy(lastUpdate = now + 1), status, now))
        assertNull(mapDisplayFix(null, snapped, status, now))
    }
}
