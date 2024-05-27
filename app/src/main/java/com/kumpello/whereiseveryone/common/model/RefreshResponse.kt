package com.kumpello.whereiseveryone.common.model

import com.squareup.moshi.JsonClass

sealed interface RefreshResponse {
    @JsonClass(generateAdapter = true)
    data class RefreshData(val token: String): RefreshResponse

    @JsonClass(generateAdapter = true)
    data class ErrorData(val code : Int, val error : String, val message : String): RefreshResponse
}