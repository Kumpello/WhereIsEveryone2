package com.kumpello.whereiseveryone.common.domain.model

import com.squareup.moshi.JsonClass

sealed interface CodeResponse {
    @JsonClass(generateAdapter = true)
    data class SuccessData(val code: Int): CodeResponse

    @JsonClass(generateAdapter = true)
    data class ErrorData(val code : Int, val error : String, val message : String): CodeResponse
}