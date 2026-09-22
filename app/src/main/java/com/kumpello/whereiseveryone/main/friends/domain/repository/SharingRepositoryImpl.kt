package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.api.SharingApi
import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
import com.kumpello.whereiseveryone.common.logging.httpFailure
import timber.log.Timber

class SharingRepositoryImpl(
    private val sharingApi: SharingApi
) : SharingRepository {

    override suspend fun stopSharing(username: String): CodeResponse {
        val response = sharingApi.stopSharing(FriendRequest(username))

        return when {
            response.isSuccessful -> {
                Timber.tag(TAG).d("Stop sharing successful")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).httpFailure("Stop sharing", response.code())
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody()?.string() ?: "",
                    response.message()
                )
            }
        }
    }

    override suspend fun resumeSharing(username: String): CodeResponse {
        val response = sharingApi.resumeSharing(FriendRequest(username))

        return when {
            response.isSuccessful -> {
                Timber.tag(TAG).d("Resume sharing successful")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).httpFailure("Resume sharing", response.code())
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody()?.string() ?: "",
                    response.message()
                )
            }
        }
    }

    override suspend fun getPausedFriends(): SharingResponse {
        val response = sharingApi.getPaused()

        return if (response.isSuccessful) {
            SharingResponse.PausedFriends(
                response.body() ?: emptyList()
            )
        } else {
            Timber.tag(TAG).httpFailure("Get paused friends", response.code())
            SharingResponse.ErrorData(
                response.code(),
                response.errorBody()?.string() ?: "",
                response.message()
            )
        }
    }

    companion object {
        private const val TAG = "SHARING_REPO"
    }
}
