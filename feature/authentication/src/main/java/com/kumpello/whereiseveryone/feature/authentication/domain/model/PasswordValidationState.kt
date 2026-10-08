package com.kumpello.whereiseveryone.feature.authentication.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class PasswordValidationState(
    val hasMinimum: Boolean = false,
    val hasCapitalizedLetter: Boolean = false,
    val hasSpecialCharacter: Boolean = false,
    val noWhitespaces: Boolean = false,
    val successful: Boolean = false
)