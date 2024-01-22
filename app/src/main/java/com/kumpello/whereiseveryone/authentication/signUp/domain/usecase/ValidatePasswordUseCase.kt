package com.kumpello.whereiseveryone.authentication.signUp.domain.usecase

import com.kumpello.whereiseveryone.authentication.signUp.domain.model.PasswordValidationState

class ValidatePasswordUseCase {

    fun execute(password: String): PasswordValidationState {

        val hasSpecialCharacter = validateSpecialCharacter(password)
        val hasCapitalizedLetter = validateCapitalizedLetter(password)
        val hasMinimum = validateMinimum(password)
        val notTooLong = validateMaximum(password)

        val hasError = listOf(
            hasSpecialCharacter,
            hasCapitalizedLetter,
            hasMinimum,
            notTooLong
        ).all { it }

        return PasswordValidationState(
            hasSpecialCharacter = hasSpecialCharacter,
            hasCapitalizedLetter = hasCapitalizedLetter,
            hasMinimum = hasMinimum,
            notTooLong = notTooLong,
            successful = hasError
        )
    }
    private fun validateSpecialCharacter(password: String): Boolean =
        password.matches(Regex("[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]"))

    private fun validateCapitalizedLetter(password: String): Boolean =
        password.matches(Regex(".*[A-Z].*"))

    private fun validateMinimum(password: String): Boolean =
        password.matches(Regex(".{$minPasswordLength,}"))

    private fun validateMaximum(password: String): Boolean =
        password.matches(Regex(".{,$maxPasswordLength}"))

    companion object {
        private const val minPasswordLength = 8
        private const val maxPasswordLength = 32
    }
}