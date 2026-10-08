package com.kumpello.whereiseveryone.feature.authentication.domain.usecase

class ValidateLoginInputUseCase {
    fun execute(input: String): String {
        return input.filter { char -> char.isLetterOrDigit() }
    }

}