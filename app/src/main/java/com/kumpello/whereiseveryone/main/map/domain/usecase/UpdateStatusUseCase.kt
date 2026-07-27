package com.kumpello.whereiseveryone.main.map.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.repository.StatusRepository

class UpdateStatusUseCase(
    private val statusRepository: StatusRepository
) {
    suspend fun execute(status: String): CodeResponse {
        return statusRepository.updateStatus(
            status = status
        )
    }
}