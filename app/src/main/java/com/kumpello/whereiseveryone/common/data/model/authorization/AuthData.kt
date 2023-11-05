package com.kumpello.whereiseveryone.common.data.model.authorization

data class AuthData(val id: String, val refresh_token: String, val token: String) : AuthResponse
