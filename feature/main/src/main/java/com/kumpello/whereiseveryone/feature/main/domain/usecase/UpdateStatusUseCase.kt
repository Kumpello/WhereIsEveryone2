package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.StatusRepository

class UpdateStatusUseCase(
    private val statusRepository: StatusRepository
) {
    suspend fun execute(status: String): CodeResponse {
        return statusRepository.updateStatus(
            status = status
        )
    }
}