package com.kumpello.whereiseveryone.authentication.login.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshRequest(val refresh_token: String)
