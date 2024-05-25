package com.kumpello.whereiseveryone.common.domain.model

import com.squareup.moshi.JsonClass

interface SuccessResponse {
    @JsonClass(generateAdapter = true)
    data class SuccessData(val code: Int): SuccessResponse
}