package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertAltUseCaseTest {

    private val useCase = ConvertAltUseCase()

    @Test
    fun `execute returns SOMEWHAT_SAME if any altitude is null`() {
        assertEquals(AltDifference.SOMEWHAT_SAME, useCase.execute(null, 100.0))
        assertEquals(AltDifference.SOMEWHAT_SAME, useCase.execute(100.0, null))
    }

    @Test
    fun `execute returns WAY_HIGHER if difference is above limit`() {
        assertEquals(AltDifference.WAY_HIGHER, useCase.execute(100.0, 160.0))
    }

    @Test
    fun `execute returns WAY_LOWER if difference is below limit`() {
        assertEquals(AltDifference.WAY_LOWER, useCase.execute(100.0, 40.0))
    }

    @Test
    fun `execute returns SOMEWHAT_SAME if difference is within limit`() {
        assertEquals(AltDifference.SOMEWHAT_SAME, useCase.execute(100.0, 110.0))
    }
}
