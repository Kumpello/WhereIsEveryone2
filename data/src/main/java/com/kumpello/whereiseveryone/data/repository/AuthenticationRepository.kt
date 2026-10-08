package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.network.model.AuthResponse

sealed interface AuthenticationRepository {
    suspend fun signUp(username: String, password: String, deviceToken: String?): AuthResponse
    suspend fun logIn(username: String, password: String, deviceToken: String?): AuthResponse

    suspend fun refreshToken(refreshToken: String, deviceToken: String?): AuthResponse
}
