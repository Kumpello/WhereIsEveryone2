package com.kumpello.whereiseveryone.common.entity

import com.kumpello.whereiseveryone.common.model.AuthResponse

data class AuthResponseWithParams(val username: String, val password: String, val authResponse: AuthResponse)
