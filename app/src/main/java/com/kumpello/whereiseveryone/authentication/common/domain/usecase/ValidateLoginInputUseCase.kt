package com.kumpello.whereiseveryone.authentication.common.domain.usecase

class ValidateLoginInputUseCase {
    fun execute(input: String): String {
        return input.filter { char -> char.isLetterOrDigit() }
    }

}