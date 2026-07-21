package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.LocationData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapLocationUseCaseTest {

    private val convertAccuracyUseCase: ConvertAccuracyUseCase = mockk()
    private val convertLastUpdateUseCase: ConvertLastUpdateUseCase = mockk()
    private val formatLastUpdateUseCase: FormatLastUpdateUseCase = mockk()

    private val useCase = MapLocationUseCase(
        convertAccuracyUseCase,
        convertLastUpdateUseCase,
        formatLastUpdateUseCase
    )

    @Test
    fun `execute maps LocationData correctly`() {
        val lastUpdate = System.currentTimeMillis()
        val data = LocationData(1.0, 2.0, 0f, 3.0, 4f, lastUpdate)

        every { convertAccuracyUseCase.execute(any()) } returns AccuracyLevel.HIGH
        every { convertLastUpdateUseCase.execute(any(), any()) } returns LastUpdateAge.FRESH
        every { formatLastUpdateUseCase.execute(any()) } returns "now"

        val result = useCase.execute(data)

        assertEquals(1.0, result.lat, 0.001)
        assertEquals(2.0, result.lon, 0.001)
        assertEquals("now", result.lastUpdateTime)
    }
}
