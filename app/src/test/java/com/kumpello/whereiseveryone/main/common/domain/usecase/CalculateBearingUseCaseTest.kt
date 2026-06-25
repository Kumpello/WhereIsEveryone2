package com.kumpello.whereiseveryone.main.common.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateBearingUseCaseTest {

    private val useCase = CalculateBearingUseCase()

    @Test
    fun `execute returns 0 for North`() {
        val bearing = useCase.execute(50.0, 20.0, 51.0, 20.0)
        assertEquals(0f, bearing, 0.1f)
    }

    @Test
    fun `execute returns 90 for East`() {
        val bearing = useCase.execute(50.0, 20.0, 50.0, 21.0)
        assertEquals(90f, bearing, 1.0f)
    }

    @Test
    fun `execute returns 180 for South`() {
        val bearing = useCase.execute(50.0, 20.0, 49.0, 20.0)
        assertEquals(180f, bearing, 0.1f)
    }

    @Test
    fun `execute returns 270 for West`() {
        val bearing = useCase.execute(50.0, 20.0, 50.0, 19.0)
        assertEquals(270f, bearing, 1.0f)
    }
}
