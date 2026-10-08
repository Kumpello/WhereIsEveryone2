package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.local.database.UserLocationDao
import com.kumpello.whereiseveryone.data.local.database.UserLocationEntity
import com.kumpello.whereiseveryone.data.model.LocationData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserLocationRepositoryTest {
    private val dao = mockk<UserLocationDao>(relaxed = true)
    private val repository = UserLocationRepository(dao)

    @Test
    fun `missing cached location stays absent`() = runTest {
        coEvery { dao.getUserLocation() } returns null
        assertNull(repository.getLastLocation())
    }

    @Test
    fun `saving and loading preserves coordinates optional measurements and fix time`() = runTest {
        val location = LocationData(
            lat = 52.0,
            lon = 21.0,
            bearing = null,
            alt = 125.0,
            accuracy = null,
            speed = 1.5f,
            lastUpdate = 1_725_000_000_123L
        )
        val entity = UserLocationEntity(
            latitude = location.lat,
            longitude = location.lon,
            bearing = location.bearing,
            altitude = location.alt,
            accuracy = location.accuracy,
            speed = location.speed,
            lastUpdate = location.lastUpdate
        )
        repository.saveLocation(location)
        coVerify(exactly = 1) { dao.updateUserLocation(entity) }
        coEvery { dao.getUserLocation() } returns entity
        assertEquals(location, repository.getLastLocation())
    }
}
