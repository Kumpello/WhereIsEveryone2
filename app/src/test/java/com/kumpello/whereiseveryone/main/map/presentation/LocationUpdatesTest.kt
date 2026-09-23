package com.kumpello.whereiseveryone.main.map.presentation

import android.location.Location
import android.os.Looper
import app.cash.turbine.test
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.TaskCompletionSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class LocationUpdatesTest {
    private val client = mockk<FusedLocationProviderClient>()
    private val request = mockk<LocationRequest>()
    private val looper = mockk<Looper>()
    private val callback = slot<LocationCallback>()
    private val registration = TaskCompletionSource<Void>()

    @Before
    fun setUp() {
        every {
            client.requestLocationUpdates(request, capture(callback), looper)
        } returns registration.task
        every { client.removeLocationUpdates(any<LocationCallback>()) } returns mockk()
    }

    @Test
    fun `registration starts only when collected and cancellation removes callback`() = runTest {
        val locations = client.locationUpdates(request, looper)
        verify(exactly = 0) { client.requestLocationUpdates(any(), any<LocationCallback>(), any()) }
        registration.setResult(null)

        locations.test {
            val location = mockk<Location>()
            val result = mockk<LocationResult> { every { lastLocation } returns location }
            callback.captured.onLocationResult(result)

            assertSame(location, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { client.removeLocationUpdates(callback.captured) }
    }

    @Test
    fun `results without a location are ignored`() = runTest {
        registration.setResult(null)

        client.locationUpdates(request, looper).test {
            callback.captured.onLocationResult(mockk { every { lastLocation } returns null })
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancellation during registration removes callback`() = runTest {
        client.locationUpdates(request, looper).test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { client.removeLocationUpdates(callback.captured) }
    }

    @Test
    fun `asynchronous registration failure propagates and removes callback`() = runTest {
        val failure = SecurityException("Location permission revoked")

        client.locationUpdates(request, looper).test {
            registration.setException(failure)
            val error = awaitError()
            assertEquals(failure.javaClass, error.javaClass)
            assertEquals(failure.message, error.message)
        }

        verify(exactly = 1) { client.removeLocationUpdates(callback.captured) }
    }

    @Test
    fun `synchronous registration failure propagates and removes callback`() = runTest {
        val failure = SecurityException("Location permission revoked")
        every {
            client.requestLocationUpdates(request, capture(callback), looper)
        } throws failure

        client.locationUpdates(request, looper).test {
            val error = awaitError()
            assertEquals(failure.javaClass, error.javaClass)
            assertEquals(failure.message, error.message)
        }

        verify(exactly = 1) { client.removeLocationUpdates(callback.captured) }
    }
}
