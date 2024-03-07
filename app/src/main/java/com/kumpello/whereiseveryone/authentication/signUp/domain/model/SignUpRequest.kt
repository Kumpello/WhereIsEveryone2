package com.kumpello.whereiseveryone.authentication.signUp.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignUpRequest(val name: String, val password: String)
