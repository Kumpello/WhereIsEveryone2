package com.kumpello.whereiseveryone.data.model.authorization

data class AuthData(val id: String, val token: String, val refresh_token: String) : AuthResponse
