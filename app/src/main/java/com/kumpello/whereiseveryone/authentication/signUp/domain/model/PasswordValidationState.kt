package com.kumpello.whereiseveryone.authentication.signUp.domain.model

data class PasswordValidationState(
    val hasMinimum: Boolean = false,
    val hasCapitalizedLetter: Boolean = false,
    val hasSpecialCharacter: Boolean = false,
    val notTooLong: Boolean = false,
    val successful: Boolean = false
)