package com.kumpello.whereiseveryone.authentication.signUp.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignUpRequest(val username: String, val password: String)
