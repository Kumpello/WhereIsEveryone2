package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.SharingRepository

class ResumeSharingUseCase(
    private val sharingRepository: SharingRepository
) {
    suspend fun execute(username: String): CodeResponse {
        return sharingRepository.resumeSharing(
            username = username
        )
    }
}
