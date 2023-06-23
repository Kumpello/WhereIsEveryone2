package com.kumpello.whereiseveryone.data.model.authorization

data class AuthData(val id: String, val refresh_token: String, val token: String) : AuthResponse
