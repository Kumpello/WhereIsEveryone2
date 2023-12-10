package com.kumpello.whereiseveryone.common.model

sealed interface AuthResponse {
    data class AuthData(val id: String, val refresh_token: String, val token: String) : AuthResponse
    class ErrorData(val code : Int, val error : String, val message : String) : AuthResponse
}