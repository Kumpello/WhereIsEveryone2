package com.kumpello.whereiseveryone.feature.main.location

import android.content.Context
import android.location.Location
import com.kumpello.whereiseveryone.core.testing.MainDispatcherRule
import com.kumpello.whereiseveryone.data.model.LocationData
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocationServiceProxyTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `binding uses latest Activity visibility and forwards subsequent changes`() {
        val service = LocationServiceProxy(mockk<Context>(relaxed = true))
        val delegate = mockk<LocationServiceProxy.LocationServiceDelegate>(relaxed = true)
        service.setAppForeground(false)
        service.registerDelegate(delegate)
        assertTrue(service.observeIsServiceRunning().value)
        verify(exactly = 1) { delegate.setAppForeground(false) }
        service.setAppForeground(true)
        verify(exactly = 1) { delegate.setAppForeground(true) }
    }

    @Test
    fun `charging mode preserves car status independently of acquisition priority`() {
        val service = LocationServiceProxy(mockk<Context>(relaxed = true))
        val status = LocationService.Status(
            updateType = LocationService.UpdateType.Charging,
            isCarMode = true,
            isCharging = true,
            isSharingEnabled = true,
        )
        service.updateStatus(status)
        assertEquals(status, service.observeStatus().value)
        val unplugged = status.copy(updateType = LocationService.UpdateType.CarMode, isCharging = false, isAppForeground = false)
        service.updateStatus(unplugged)
        assertEquals(unplugged, service.observeStatus().value)
    }

    @Test
    fun `Mapbox display correction leaves shared GPS data untouched`() {
        val service = LocationServiceProxy(mockk<Context>(relaxed = true))
        val raw = mockk<Location>()
        service.updateLocation(raw)
        service.updateStatus(LocationService.Status(isCarMode = true))
        service.setMapDisplayLocation(LocationData(52.0, 21.0, 90f, null, 10f, 20f, 1_800_000_000_000L, true))
        assertSame(raw, service.observeLocation().value)
        service.setMapDisplayLocation(null)
        assertSame(raw, service.observeLocation().value)
    }
}
