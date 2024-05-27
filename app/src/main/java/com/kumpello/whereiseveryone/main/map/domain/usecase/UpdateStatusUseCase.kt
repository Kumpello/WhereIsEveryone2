package com.kumpello.whereiseveryone.main.map.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.main.map.domain.repository.StatusRepository

class UpdateStatusUseCase(
    private val statusRepository: StatusRepository,
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
) {
    fun execute(message: String): CodeResponse {
        return statusRepository.updateStatus(
            token = getCurrentAuthKeyUseCase.execute().toString(),
            message = message
        )
    }
}