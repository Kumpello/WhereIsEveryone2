package com.kumpello.whereiseveryone.feature.main.ui.map

import app.cash.turbine.test
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UserLocationMarkerTest {
    @Test
    fun `marker waits for a fix and follows animated positions and bearings`() = runTest {
        val location = mockk<LocationComponentPlugin>(relaxed = true)
        val positionListener = slot<OnIndicatorPositionChangedListener>()
        val bearingListener = slot<OnIndicatorBearingChangedListener>()
        every { location.addOnIndicatorPositionChangedListener(capture(positionListener)) } returns Unit
        every { location.addOnIndicatorBearingChangedListener(capture(bearingListener)) } returns Unit

        location.userMarkerFeatures().test {
            bearingListener.captured.onIndicatorBearingChanged(123.5)
            expectNoEvents()

            val initial = Point.fromLngLat(17.032123456, 51.086123456)
            positionListener.captured.onIndicatorPositionChanged(initial)
            val first = awaitItem()
            assertEquals(initial, first.geometry())
            assertEquals(123.5, first.getNumberProperty("bearing").toDouble(), 0.0)

            val moved = Point.fromLngLat(17.032123789, 51.086123789)
            positionListener.captured.onIndicatorPositionChanged(moved)
            val second = awaitItem()
            assertEquals(first.id(), second.id())
            assertEquals(moved, second.geometry())

            bearingListener.captured.onIndicatorBearingChanged(124.0)
            val turned = awaitItem()
            assertEquals(moved, turned.geometry())
            assertEquals(124.0, turned.getNumberProperty("bearing").toDouble(), 0.0)

            positionListener.captured.onIndicatorPositionChanged(moved)
            bearingListener.captured.onIndicatorBearingChanged(124.0)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { location.removeOnIndicatorPositionChangedListener(positionListener.captured) }
        verify(exactly = 1) { location.removeOnIndicatorBearingChangedListener(bearingListener.captured) }
    }

    @Test
    fun `marker immediately uses cached SDK location after rebinding`() = runTest {
        val location = mockk<LocationComponentPlugin>(relaxed = true)
        val point = Point.fromLngLat(17.032123456, 51.086123456)
        every { location.addOnIndicatorBearingChangedListener(any()) } answers {
            firstArg<OnIndicatorBearingChangedListener>().onIndicatorBearingChanged(270.0)
        }
        every { location.addOnIndicatorPositionChangedListener(any()) } answers {
            firstArg<OnIndicatorPositionChangedListener>().onIndicatorPositionChanged(point)
        }

        repeat(2) {
            location.userMarkerFeatures().test {
                val marker = awaitItem()
                assertEquals(point, marker.geometry())
                assertEquals(270.0, marker.getNumberProperty("bearing").toDouble(), 0.0)
                cancelAndIgnoreRemainingEvents()
            }
        }
        verify(exactly = 2) { location.removeOnIndicatorPositionChangedListener(any()) }
        verify(exactly = 2) { location.removeOnIndicatorBearingChangedListener(any()) }
    }
}
