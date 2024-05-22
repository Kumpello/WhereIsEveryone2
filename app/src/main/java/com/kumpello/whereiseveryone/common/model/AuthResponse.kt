package com.kumpello.whereiseveryone.common.model

import com.squareup.moshi.JsonClass

interface AuthResponse {
    @JsonClass(generateAdapter = true)
    data class AuthData(val id: String, val refresh_token: String, val token: String) : AuthResponse

}