package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.LocationData
import org.junit.Assert.assertEquals
import org.junit.Test

class MapLocationUseCaseTest {

    private val useCase = MapLocationUseCase()

    @Test
    fun `execute maps LocationData correctly`() {
        val lastUpdate = System.currentTimeMillis()
        val data = LocationData(1.0, 2.0, 0f, 3.0, 4f, lastUpdate)

        val result = useCase.execute(data)

        assertEquals(1.0, result.lat, 0.001)
        assertEquals(2.0, result.lon, 0.001)
        // Values from LocationUtils
        assertEquals(3.0, result.rawAlt!!, 0.001)
    }
}
