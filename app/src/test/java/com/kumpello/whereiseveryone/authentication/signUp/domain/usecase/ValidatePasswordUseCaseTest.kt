package com.kumpello.whereiseveryone.authentication.signUp.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatePasswordUseCaseTest {

    private val useCase = ValidatePasswordUseCase()

    @Test
    fun `execute returns successful when password meets all criteria`() {
        val result = useCase.execute("ValidPass123!")
        assertTrue(result.hasSpecialCharacter)
        assertTrue(result.hasCapitalizedLetter)
        assertTrue(result.hasMinimum)
        assertTrue(result.noWhitespaces)
        assertTrue(result.successful)
    }

    @Test
    fun `execute returns failure when missing special character`() {
        val result = useCase.execute("InvalidPass123")
        assertFalse(result.hasSpecialCharacter)
        assertFalse(result.successful)
    }

    @Test
    fun `execute returns failure when missing capitalized letter`() {
        val result = useCase.execute("invalidpass123!")
        assertFalse(result.hasCapitalizedLetter)
        assertFalse(result.successful)
    }

    @Test
    fun `execute returns failure when too short`() {
        val result = useCase.execute("Vp1!")
        assertFalse(result.hasMinimum)
        assertFalse(result.successful)
    }

    @Test
    fun `execute returns failure when containing whitespaces`() {
        val result = useCase.execute("Valid Pass123!")
        assertFalse(result.noWhitespaces)
        assertFalse(result.successful)
    }
}
