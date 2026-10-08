package com.kumpello.whereiseveryone.data.model

import com.kumpello.whereiseveryone.data.network.model.AuthResponse

data class AuthResponseWithParams(val username: String, val password: String, val authResponse: AuthResponse)
