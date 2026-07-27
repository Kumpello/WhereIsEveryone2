package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse

interface SharingRepository {
    suspend fun stopSharing(username: String): CodeResponse
    suspend fun resumeSharing(username: String): CodeResponse
    suspend fun getPausedFriends(): SharingResponse
}
