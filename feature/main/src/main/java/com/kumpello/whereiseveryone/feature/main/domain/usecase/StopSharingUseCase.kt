package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.SharingRepository

class StopSharingUseCase(
    private val sharingRepository: SharingRepository
) {
    suspend fun execute(username: String): CodeResponse {
        return sharingRepository.stopSharing(
            username = username
        )
    }
}
