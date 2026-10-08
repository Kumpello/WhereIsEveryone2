package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.CodeResponse

sealed interface StatusRepository {
    suspend fun updateStatus(status: String): CodeResponse
}