package com.kumpello.whereiseveryone.common.entities

import com.kumpello.whereiseveryone.common.data.model.authorization.AuthResponse

data class AuthResponseWithParams(val username: String, val password: String, val authResponse: AuthResponse)
