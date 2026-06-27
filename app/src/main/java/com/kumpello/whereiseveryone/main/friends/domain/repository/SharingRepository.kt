package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse

interface SharingRepository {
    suspend fun stopSharing(token: String, username: String): CodeResponse
    suspend fun resumeSharing(token: String, username: String): CodeResponse
    suspend fun getPausedFriends(token: String): SharingResponse
}
