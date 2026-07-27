package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse

sealed interface StatusRepository {
    suspend fun updateStatus(status: String): CodeResponse
}