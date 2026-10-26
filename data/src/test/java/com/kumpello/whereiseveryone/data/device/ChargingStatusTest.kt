package com.kumpello.whereiseveryone.data.device

import android.os.BatteryManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingStatusTest {
    @Test
    fun `connected power includes charging and full battery but unplugging always disables it`() {
        for (status in listOf(BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL)) {
            assertTrue(isBatteryCharging(status, BatteryManager.BATTERY_PLUGGED_AC))
            assertTrue(isBatteryCharging(status, BatteryManager.BATTERY_PLUGGED_USB))
            assertTrue(isBatteryCharging(status, BatteryManager.BATTERY_PLUGGED_WIRELESS))
            assertFalse(isBatteryCharging(status, 0))
        }
        assertFalse(isBatteryCharging(BatteryManager.BATTERY_STATUS_DISCHARGING, 0))
        assertFalse(isBatteryCharging(BatteryManager.BATTERY_STATUS_UNKNOWN, BatteryManager.BATTERY_PLUGGED_AC))
    }
}
