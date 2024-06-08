package com.kumpello.whereiseveryone.common.model

import com.squareup.moshi.JsonClass

sealed interface AuthResponse { //TODO: Looks like shit, @JsonName or configure generateAdapter to change names via
    @JsonClass(generateAdapter = true)
    data class AuthData(val id: String, val refresh_token: String, val token: String) : AuthResponse

    @JsonClass(generateAdapter = true)
    data class ErrorData(val code : Int, val error : String, val message : String): AuthResponse
}