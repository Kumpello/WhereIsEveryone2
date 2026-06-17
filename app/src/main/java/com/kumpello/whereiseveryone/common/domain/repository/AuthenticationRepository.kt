package com.kumpello.whereiseveryone.common.domain.repository

import com.kumpello.whereiseveryone.common.model.AuthResponse

sealed interface AuthenticationRepository {
    suspend fun signUp(username: String, password: String): AuthResponse
    suspend fun logIn(username: String, password: String): AuthResponse

    suspend fun refreshToken(refreshToken: String): AuthResponse
}