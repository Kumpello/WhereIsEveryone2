package com.kumpello.whereiseveryone.data.model

import com.squareup.moshi.JsonClass

sealed interface CodeResponse {

    data object SuccessNoContent: CodeResponse

    @JsonClass(generateAdapter = true)
    data class ErrorData(val code : Int, val error : String, val message : String): CodeResponse
}