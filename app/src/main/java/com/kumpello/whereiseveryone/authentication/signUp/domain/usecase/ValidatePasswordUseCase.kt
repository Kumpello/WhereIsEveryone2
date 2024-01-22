package com.kumpello.whereiseveryone.authentication.signUp.domain.usecase

import com.kumpello.whereiseveryone.authentication.signUp.domain.model.PasswordValidationState

class ValidatePasswordUseCase {

    fun execute(password: String): PasswordValidationState {

        val hasSpecialCharacter = validateSpecialCharacter(password)
        val hasCapitalizedLetter = validateCapitalizedLetter(password)
        val hasMinimum = validateMinimum(password)
        val noWhitespaces = validateWhitespaces(password)

        val hasError = listOf(
            hasSpecialCharacter,
            hasCapitalizedLetter,
            hasMinimum,
            noWhitespaces
        ).all { it }

        return PasswordValidationState(
            hasSpecialCharacter = hasSpecialCharacter,
            hasCapitalizedLetter = hasCapitalizedLetter,
            hasMinimum = hasMinimum,
            noWhitespaces = noWhitespaces,
            successful = hasError
        )
    }

    private fun validateSpecialCharacter(password: String): Boolean =
        password.any { character ->
            character.isLetterOrDigit().not() && character.isWhitespace().not()
        }

    private fun validateWhitespaces(password: String): Boolean =
        password.any { character ->
            character.isWhitespace().not()
        }

    private fun validateCapitalizedLetter(password: String): Boolean =
        password.any { character ->
            character.isUpperCase()
        }

    private fun validateMinimum(password: String): Boolean =
        password.length > minPasswordLength

    companion object {
        private const val minPasswordLength = 8
        private const val maxPasswordLength = 32
    }
}