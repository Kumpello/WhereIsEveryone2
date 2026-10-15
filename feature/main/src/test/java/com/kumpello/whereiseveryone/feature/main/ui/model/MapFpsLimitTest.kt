package com.kumpello.whereiseveryone.feature.main.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MapFpsLimitTest {
    @Test
    fun `60 Hz display disables 90 and 120 FPS`() {
        assertEquals(listOf(30, 60), supportedFps(60f))
    }

    @Test
    fun `90 Hz display enables 90 FPS and disables 120 FPS`() {
        assertEquals(listOf(30, 60, 90), supportedFps(90f))
    }

    @Test
    fun `120 Hz and faster displays enable all limits`() {
        assertEquals(listOf(30, 60, 90, 120), supportedFps(120f))
        assertEquals(listOf(30, 60, 90, 120), supportedFps(144f))
    }

    @Test
    fun `fractional display rates enable their nominal FPS limit`() {
        assertEquals(listOf(30, 60), supportedFps(59.94f))
        assertEquals(listOf(30, 60, 90), supportedFps(89.91f))
        assertEquals(listOf(30, 60, 90, 120), supportedFps(119.88f))
    }

    @Test
    fun `saved limit adapts to a slower display without raising lower preferences`() {
        assertEquals(MapFpsLimit.FPS_60, MapFpsLimit.FPS_120.forDisplay(60f))
        assertEquals(MapFpsLimit.FPS_90, MapFpsLimit.FPS_120.forDisplay(90f))
        assertEquals(MapFpsLimit.FPS_30, MapFpsLimit.FPS_30.forDisplay(120f))
    }

    @Test
    fun `missing or invalid saved FPS defaults to highest supported display option`() {
        listOf(null, 0, 45).forEach { savedFps ->
            val defaultLimit = MapFpsLimit.fromPreference(savedFps)
            assertEquals(MapFpsLimit.FPS_60, defaultLimit.forDisplay(60f))
            assertEquals(MapFpsLimit.FPS_90, defaultLimit.forDisplay(90f))
            assertEquals(MapFpsLimit.FPS_120, defaultLimit.forDisplay(120f))
            assertEquals(MapFpsLimit.FPS_120, defaultLimit.forDisplay(144f))
        }
    }

    private fun supportedFps(refreshRate: Float): List<Int> {
        return MapFpsLimit.entries.filter { it.isSupported(refreshRate) }.map { it.fps }
    }
}
