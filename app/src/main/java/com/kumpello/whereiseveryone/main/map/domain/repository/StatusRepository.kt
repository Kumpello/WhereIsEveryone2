package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.SuccessResponse

sealed interface StatusRepository {
    fun updateStatus(token: String, message: String): SuccessResponse
}