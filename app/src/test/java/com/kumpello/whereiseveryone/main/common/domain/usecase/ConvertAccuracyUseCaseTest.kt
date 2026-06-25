package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertAccuracyUseCaseTest {

    private val useCase = ConvertAccuracyUseCase()

    @Test
    fun `execute returns UNKNOWN if accuracy is null`() {
        assertEquals(AccuracyLevel.UNKNOWN, useCase.execute(null))
    }

    @Test
    fun `execute returns TRAGIC for high accuracy values`() {
        assertEquals(AccuracyLevel.TRAGIC, useCase.execute(40f))
    }

    @Test
    fun `execute returns PERFECT for very low accuracy values`() {
        assertEquals(AccuracyLevel.PERFECT, useCase.execute(1f))
    }
}
