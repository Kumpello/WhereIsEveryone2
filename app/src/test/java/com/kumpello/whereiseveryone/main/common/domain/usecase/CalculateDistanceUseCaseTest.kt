package com.kumpello.whereiseveryone.main.common.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateDistanceUseCaseTest {

    private val useCase = CalculateDistanceUseCase()

    @Test
    fun `execute returns zero distance for same coordinates`() {
        val dist = useCase.execute(50.0, 20.0, 100.0, 50.0, 20.0, 100.0)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun `execute returns correct horizontal distance`() {
        // Roughly 1 degree of latitude is 111km
        val dist = useCase.execute(50.0, 20.0, 0.0, 51.0, 20.0, 0.0)
        assertEquals(111194.9, dist, 10.0)
    }

    @Test
    fun `execute returns correct vertical distance`() {
        val dist = useCase.execute(50.0, 20.0, 0.0, 50.0, 20.0, 100.0)
        assertEquals(100.0, dist, 0.001)
    }
}
