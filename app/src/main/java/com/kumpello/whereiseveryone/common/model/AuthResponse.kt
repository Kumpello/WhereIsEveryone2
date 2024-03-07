package com.kumpello.whereiseveryone.common.model

import com.squareup.moshi.JsonClass

sealed interface AuthResponse {
    @JsonClass(generateAdapter = true)
    data class AuthData(val id: String, val refresh_token: String, val token: String) : AuthResponse
    @JsonClass(generateAdapter = true)
    data class ErrorData(val code : Int, val error : String, val message : String) : AuthResponse
}