package com.kumpello.whereiseveryone.main.common.util

import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun `convertAccuracy returns correct level`() {
        assertEquals(AccuracyLevel.PERFECT, LocationUtils.convertAccuracy(2f))
        assertEquals(AccuracyLevel.HIGH, LocationUtils.convertAccuracy(5f))
        assertEquals(AccuracyLevel.MEDIUM, LocationUtils.convertAccuracy(10f))
        assertEquals(AccuracyLevel.LOW, LocationUtils.convertAccuracy(20f))
        assertEquals(AccuracyLevel.TRAGIC, LocationUtils.convertAccuracy(40f))
        assertEquals(AccuracyLevel.UNKNOWN, LocationUtils.convertAccuracy(null))
    }

    @Test
    fun `calculateDistance returns reasonable results`() {
        // Distance between (0,0) and (1,0) is approx 111.19 km = 111190 m
        val dist = LocationUtils.calculateDistance(0.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        assertEquals(111194.9, dist, 1.0)
    }

    @Test
    fun `calculateBearing returns correct degrees`() {
        // (0,0) to (1,0) is North (0 deg)
        assertEquals(0f, LocationUtils.calculateBearing(0.0, 0.0, 1.0, 0.0), 0.1f)
        // (0,0) to (0,1) is East (90 deg)
        assertEquals(90f, LocationUtils.calculateBearing(0.0, 0.0, 0.0, 1.0), 0.1f)
    }

    @Test
    fun `convertAlt returns correct AltDifference`() {
        assertEquals(AltDifference.SOMEWHAT_SAME, LocationUtils.convertAlt(100.0, 120.0))
        assertEquals(AltDifference.WAY_HIGHER, LocationUtils.convertAlt(100.0, 160.0))
        assertEquals(AltDifference.WAY_LOWER, LocationUtils.convertAlt(100.0, 40.0))
    }

    @Test
    fun `convertLastUpdate returns correct age`() {
        val now = System.currentTimeMillis()
        assertEquals(LastUpdateAge.FRESH, LocationUtils.convertLastUpdate(now - 10_000, now))
        assertEquals(LastUpdateAge.NEW, LocationUtils.convertLastUpdate(now - 120_000, now))
        assertEquals(LastUpdateAge.OLD_AS_FUCK, LocationUtils.convertLastUpdate(now - 4_000_000, now))
    }
}
