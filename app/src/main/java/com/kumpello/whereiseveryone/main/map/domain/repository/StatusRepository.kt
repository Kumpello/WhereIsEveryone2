package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.model.ErrorData

sealed interface StatusRepository {
    fun updateStatus(message: String): ErrorData?
}