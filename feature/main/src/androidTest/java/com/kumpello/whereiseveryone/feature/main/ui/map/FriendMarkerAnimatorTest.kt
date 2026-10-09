package com.kumpello.whereiseveryone.feature.main.ui.map

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FriendMarkerAnimatorTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val initial = AnimatedFriendData(
        lat = 52.2296756,
        lon = 21.0122287,
        bearing = 359.0,
        opacity = 1.0,
        haloWidth = 1.0,
        speed = 0.0,
    )

    @Test
    fun stationaryMarkerDoesNotScheduleAnimationFrames() {
        var updates = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AnimatedFriendEffect("alice", initial, onUpdate = { updates++ }, onRemoved = {})
        }
        composeRule.runOnIdle { assertEquals(1, updates) }
        composeRule.mainClock.advanceTimeBy(1200)
        composeRule.runOnIdle { assertEquals(1, updates) }
    }

    @Test
    fun movementPreservesDoublePrecisionAndUsesShortestBearingRotation() {
        val target = mutableStateOf(initial)
        var displayed = initial
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AnimatedFriendEffect("alice", target.value, onUpdate = { displayed = it }, onRemoved = {})
        }
        val destination = initial.copy(lat = initial.lat + 0.000001, lon = initial.lon + 0.000001, bearing = 1.0)
        composeRule.runOnIdle { target.value = destination }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.runOnIdle {
            assertTrue(displayed.lat > initial.lat && displayed.lat < destination.lat)
            assertTrue(displayed.bearing in 359.0..361.0)
        }
        composeRule.mainClock.advanceTimeBy(800)
        composeRule.runOnIdle {
            assertEquals(destination.lat, displayed.lat, 0.0)
            assertEquals(destination.lon, displayed.lon, 0.0)
            assertEquals(destination.bearing, displayed.bearing, 0.0)
        }
    }

    @Test
    fun fadingDoesNotRestartAnOngoingPositionAnimation() {
        val target = mutableStateOf(initial)
        var displayed = initial
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AnimatedFriendEffect("alice", target.value, onUpdate = { displayed = it }, onRemoved = {})
        }
        val destination = initial.copy(lat = initial.lat + 0.001)
        composeRule.runOnIdle { target.value = destination }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.runOnIdle { target.value = destination.copy(opacity = 0.3) }
        composeRule.mainClock.advanceTimeBy(650)
        composeRule.runOnIdle { assertEquals(destination.lat, displayed.lat, 0.0) }
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.runOnIdle { assertEquals(0.3, displayed.opacity, 0.000001) }
    }
}
