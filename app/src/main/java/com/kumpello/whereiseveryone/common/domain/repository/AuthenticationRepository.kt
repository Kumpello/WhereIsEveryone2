package com.kumpello.whereiseveryone.common.domain.repository

import com.kumpello.whereiseveryone.common.model.AuthResponse

interface AuthenticationRepository {
    fun signUp(username: String, password: String): AuthResponse
    fun logIn(username: String, password: String): AuthResponse
}