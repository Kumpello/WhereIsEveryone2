package com.kumpello.whereiseveryone.main.map.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.repository.StatusRepository

class UpdateStatusUseCase(
    private val statusRepository: StatusRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend fun execute(status: String): CodeResponse {
        return statusRepository.updateStatus(
            token = preferencesManager.get(PreferencesKey.AuthToken).toString(),
            status = status
        )
    }
}