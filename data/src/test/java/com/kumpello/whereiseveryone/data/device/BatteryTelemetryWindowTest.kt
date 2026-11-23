package com.kumpello.whereiseveryone.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryTelemetryWindowTest {
    @Test
    fun `mode boundary includes the entire preceding window and starts a new one`() {
        val window = BatteryTelemetryWindow()
        assertNull(window.update(snapshot(1_000, 80, 3_000_000), "foreground"))
        assertNull(window.update(snapshot(2_000, 79, 2_990_000), "foreground"))

        val foreground = requireNotNull(window.update(snapshot(4_000, 78, 2_980_000), "background"))
        assertEquals(3_000L, foreground.durationMs)
        assertEquals(80, foreground.startLevelPercent)
        assertEquals(78, foreground.endLevelPercent)
        assertEquals(3_000_000L, foreground.startChargeUah)
        assertEquals(2_980_000L, foreground.endChargeUah)

        val background = requireNotNull(window.update(snapshot(8_000, 77, 2_970_000), "car"))
        assertEquals(4_000L, background.durationMs)
        assertEquals(78, background.startLevelPercent)
        assertEquals(77, background.endLevelPercent)
    }

    @Test
    fun `charging boundaries never combine plugged and unplugged windows`() {
        val window = BatteryTelemetryWindow()
        window.update(snapshot(0, 50, null), "foreground")
        val discharge = requireNotNull(window.update(snapshot(5_000, 49, null, charging = true), "foreground"))
        assertFalse(discharge.charging)
        assertNull(discharge.startChargeUah)
        assertNull(discharge.endChargeUah)

        val charge = requireNotNull(window.update(snapshot(12_000, 52, null), "foreground"))
        assertTrue(charge.charging)
        assertEquals(7_000L, charge.durationMs)
        assertEquals(49, charge.startLevelPercent)
        assertEquals(52, charge.endLevelPercent)
    }

    @Test
    fun `periodic flush starts the next baseline without needing a mode change`() {
        val window = BatteryTelemetryWindow()
        window.update(snapshot(10, 60, null), "background")
        assertEquals(900_000L, requireNotNull(window.update(
            snapshot(900_010, 58, null), "background", flush = true,
        )).durationMs)
        assertEquals(100L, requireNotNull(window.update(
            snapshot(900_110, 58, null), "inactive",
        )).durationMs)
    }

    @Test
    fun `unavailable measurements reset the baseline instead of attributing an unknown gap`() {
        val window = BatteryTelemetryWindow()
        window.update(snapshot(0, 60, null), "foreground")
        window.reset()
        assertNull(window.update(snapshot(10_000, 50, null), "car"))
        assertEquals(1_000L, requireNotNull(window.update(
            snapshot(11_000, 49, null), "inactive",
        )).durationMs)
    }

    @Test
    fun `zero or negative elapsed time never produces a consumption sample`() {
        val window = BatteryTelemetryWindow()
        window.update(snapshot(1_000, 60, null), "foreground")
        assertNull(window.update(snapshot(1_000, 60, null), "car"))
        assertNull(window.update(snapshot(999, 60, null), "background"))
    }

    private fun snapshot(
        elapsed: Long,
        level: Int,
        charge: Long?,
        charging: Boolean = false,
    ) = BatteryTelemetrySnapshot(elapsed, level, charging, charge)
}
