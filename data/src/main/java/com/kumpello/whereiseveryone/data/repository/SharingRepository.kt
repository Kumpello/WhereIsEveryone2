package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.network.model.SharingResponse

interface SharingRepository {
    suspend fun stopSharing(username: String): CodeResponse
    suspend fun resumeSharing(username: String): CodeResponse
    suspend fun getPausedFriends(): SharingResponse
}
