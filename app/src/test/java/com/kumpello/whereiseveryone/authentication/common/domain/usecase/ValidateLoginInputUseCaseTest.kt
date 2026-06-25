package com.kumpello.whereiseveryone.authentication.common.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateLoginInputUseCaseTest {

    private val useCase = ValidateLoginInputUseCase()

    @Test
    fun `execute filters non-alphanumeric characters`() {
        val input = "user_123!@#"
        val expected = "user123"
        assertEquals(expected, useCase.execute(input))
    }

    @Test
    fun `execute returns same string if already alphanumeric`() {
        val input = "user123"
        assertEquals(input, useCase.execute(input))
    }

    @Test
    fun `execute returns empty string for purely special characters`() {
        val input = "!@#$%^&*()"
        assertEquals("", useCase.execute(input))
    }
}
