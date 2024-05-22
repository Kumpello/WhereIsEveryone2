package com.kumpello.whereiseveryone.common.model

import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErrorData(val code : Int, val error : String, val message : String)
    : AuthResponse, RefreshResponse, PositionsResponse