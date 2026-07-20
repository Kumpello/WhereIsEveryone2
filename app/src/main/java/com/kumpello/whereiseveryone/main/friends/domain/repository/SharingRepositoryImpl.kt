package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.api.SharingApi
import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
import timber.log.Timber

class SharingRepositoryImpl(
    private val sharingApi: SharingApi
) : SharingRepository {

    override suspend fun stopSharing(token: String, username: String): CodeResponse {
        return try {
            val response = sharingApi.stopSharing("Bearer $token", FriendRequest(username))

            when {
                response.isSuccessful -> {
                    Timber.tag(TAG).d("Stop sharing successful for user: %s", username)
                    CodeResponse.SuccessNoContent
                }

                else -> {
                    Timber.tag(TAG).e("Stop sharing failed: %s", response.errorBody()?.string())
                    CodeResponse.ErrorData(
                        response.code(),
                        response.errorBody()?.string() ?: "",
                        response.message()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Stop sharing failed with exception")
            CodeResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    override suspend fun resumeSharing(token: String, username: String): CodeResponse {
        return try {
            val response = sharingApi.resumeSharing("Bearer $token", FriendRequest(username))

            when {
                response.isSuccessful -> {
                    Timber.tag(TAG).d("Resume sharing successful for user: %s", username)
                    CodeResponse.SuccessNoContent
                }

                else -> {
                    Timber.tag(TAG).e("Resume sharing failed: %s", response.errorBody()?.string())
                    CodeResponse.ErrorData(
                        response.code(),
                        response.errorBody()?.string() ?: "",
                        response.message()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Resume sharing failed with exception")
            CodeResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    override suspend fun getPausedFriends(token: String): SharingResponse {
        return try {
            val response = sharingApi.getPaused("Bearer $token")

            if (response.isSuccessful) {
                SharingResponse.PausedFriends(
                    response.body() ?: emptyList()
                )
            } else {
                Timber.tag(TAG).e("Get paused friends failed: %s", response.errorBody()?.string())
                SharingResponse.ErrorData(
                    response.code(),
                    response.errorBody()?.string() ?: "",
                    response.message()
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Get paused friends failed with exception")
            SharingResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    companion object {
        private const val TAG = "SHARING_REPO"
    }
}
