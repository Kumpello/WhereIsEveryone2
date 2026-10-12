package com.kumpello.whereiseveryone.feature.main.ui.map

import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.repository.RoadMatch
import com.kumpello.whereiseveryone.data.repository.RoadPoint
import com.kumpello.whereiseveryone.data.repository.RoadInterceptionEstimate
import org.junit.Assert.*
import org.junit.Test

class CarModeTest {
    private val now = 1_800_000_000_000L
    private val fix = LocationData(52.0, 21.0, 0f, null, 20f, 10f, now, isCarMode = true)

    @Test
    fun `suggestion requires fresh known speed strictly above 30 kmh`() {
        assertTrue(shouldSuggestCarMode(fix.copy(speed = 8.34f), now))
        assertFalse(shouldSuggestCarMode(fix.copy(speed = 8.33f), now))
        assertFalse(shouldSuggestCarMode(fix.copy(speed = null), now))
        assertFalse(shouldSuggestCarMode(fix.copy(speed = Float.NaN), now))
        assertFalse(shouldSuggestCarMode(fix.copy(speed = Float.POSITIVE_INFINITY), now))
        assertFalse(shouldSuggestCarMode(fix.copy(lastUpdate = now - 10_001L), now))
    }

    @Test
    fun `snap projects onto recent road within accuracy without changing measured speed or fix time`() {
        val match = RoadMatch(fix, RoadPoint(52.0, 21.0), listOf(RoadPoint(51.999, 21.0), RoadPoint(52.001, 21.0)))
        val drifted = fix.copy(lon = 21.0001, lastUpdate = now + 500L)
        val displayed = roadDisplayLocation(drifted, match, now + 500L)
        assertEquals(21.0, displayed.lon, 0.00000001)
        assertEquals(drifted.lat, displayed.lat, 0.00000001)
        assertEquals(drifted.speed, displayed.speed)
        assertEquals(drifted.lastUpdate, displayed.lastUpdate)
    }

    @Test
    fun `stale far away and non car locations keep GPS position`() {
        val match = RoadMatch(fix, RoadPoint(52.0, 21.0), listOf(RoadPoint(51.999, 21.0), RoadPoint(52.001, 21.0)))
        val stale = fix.copy(lastUpdate = now + 10_001L)
        assertEquals(stale, roadDisplayLocation(stale, match, stale.lastUpdate))
        val distant = fix.copy(lon = 21.01)
        assertEquals(distant, roadDisplayLocation(distant, match, now))
        val pedestrian = fix.copy(isCarMode = false, lon = 21.0001)
        assertEquals(pedestrian, roadDisplayLocation(pedestrian, match, now))
        assertEquals(fix, roadDisplayLocation(fix, null, now))
    }

    @Test
    fun `prediction counts down using request time and allows wrapped bearings`() {
        val user = fix.copy(speed = 20f, bearing = 359f)
        val prediction = CarInterceptionPrediction(RoadInterceptionEstimate.Intercept(100.0, 2_000.0, RoadPoint(52.01, 21.0)), user, fix, now)
        val remaining = prediction.currentEstimate(user.copy(bearing = 1f), fix, now + 10_000) as RoadInterceptionEstimate.Intercept
        assertEquals(90.0, remaining.seconds, 0.0)
        assertNull(prediction.currentEstimate(user, fix, now + 30_001))
        assertNull(prediction.currentEstimate(user, fix, now - 1))
    }

    @Test
    fun `changing speed heading or mode invalidates the old interception`() {
        val user = fix.copy(speed = 20f)
        val prediction = CarInterceptionPrediction(RoadInterceptionEstimate.Intercept(100.0, 2_000.0, RoadPoint(52.01, 21.0)), user, fix, now)
        assertNull(prediction.currentEstimate(user.copy(speed = 0f), fix, now))
        assertNull(prediction.currentEstimate(user, fix.copy(speed = 25f), now))
        assertNull(prediction.currentEstimate(user, fix.copy(bearing = 90f), now))
        assertNull(prediction.currentEstimate(user, fix.copy(speed = null), now))
        assertNull(prediction.currentEstimate(user.copy(isCarMode = false), fix, now))
        val slow = user.copy(speed = 0.5f)
        val slowPrediction = prediction.copy(user = slow)
        assertNull(slowPrediction.currentEstimate(slow.copy(speed = 0f), fix, now))
    }

    @Test
    fun `lateness stays constant while arrival counts down and passed interception expires`() {
        val user = fix.copy(speed = 20f)
        val point = RoadPoint(52.01, 21.0)
        val late = CarInterceptionPrediction(RoadInterceptionEstimate.ArriveLater(100.0, 30.0, 2_000.0, point), user, fix, now)
        val remaining = late.currentEstimate(user, fix, now + 10_000) as RoadInterceptionEstimate.ArriveLater
        assertEquals(90.0, remaining.arrivalSeconds, 0.0)
        assertEquals(30.0, remaining.secondsAfterDriver, 0.0)
        val interception = late.copy(estimate = RoadInterceptionEstimate.Intercept(5.0, 100.0, point))
        assertNull(interception.currentEstimate(user, fix, now + 6_000))
        val together = late.copy(estimate = RoadInterceptionEstimate.Intercept(0.0, 0.0, point))
        assertNull(together.currentEstimate(user.copy(lat = 52.1), fix, now))
    }
}
