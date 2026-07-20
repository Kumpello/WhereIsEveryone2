package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.api.FriendApi
import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import timber.log.Timber

class FriendRepositoryImpl(
    private val friendApi: FriendApi
) : FriendRepository {

    override suspend fun addFriend(token: String, username: String): CodeResponse {
        return try {
            val response = friendApi.addFriend("Bearer $token", FriendRequest(username))

            when {
                response.isSuccessful -> {
                    Timber.tag(TAG).d("Add friend request successful for user: %s", username)
                    CodeResponse.SuccessNoContent
                }

                else -> {
                    Timber.tag(TAG).e("Add friend request failed: %s", response.errorBody()?.string())
                    CodeResponse.ErrorData(
                        response.code(),
                        response.errorBody().toString(),
                        response.message()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Add friend request failed with exception")
            CodeResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    override suspend fun removeFriend(token: String, username: String): CodeResponse {
        return try {
            val response = friendApi.removeFriend("Bearer $token", FriendRequest(username))

            when {
                response.isSuccessful -> {
                    Timber.tag(TAG).d("Remove friend successful for user: %s", username)
                    CodeResponse.SuccessNoContent
                }

                else -> {
                    Timber.tag(TAG).e("Remove friend failed: %s", response.errorBody()?.string())
                    CodeResponse.ErrorData(
                        response.code(),
                        response.errorBody().toString(),
                        response.message()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Remove friend failed with exception")
            CodeResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    override suspend fun acceptFriendRequest(
        token: String,
        username: String
    ): CodeResponse {
        return try {
            val response =
                friendApi.acceptFriendRequest("Bearer $token", FriendRequest(username))

            when {
                response.isSuccessful -> {
                    Timber.tag(TAG).d("Accept friend request successful for user: %s", username)
                    CodeResponse.SuccessNoContent
                }

                else -> {
                    Timber.tag(TAG).e("Accept friend request failed: %s", response.errorBody()?.string())
                    CodeResponse.ErrorData(
                        response.code(),
                        response.errorBody().toString(),
                        response.message()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Accept friend request failed with exception")
            CodeResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    override suspend fun rejectFriendRequest(
        token: String,
        username: String
    ): CodeResponse {
        return try {
            val response =
                friendApi.rejectFriendRequest("Bearer $token", FriendRequest(username))

            when {
                response.isSuccessful -> {
                    Timber.tag(TAG).d("Reject friend request successful for user: %s", username)
                    CodeResponse.SuccessNoContent
                }

                else -> {
                    Timber.tag(TAG).e("Reject friend request failed: %s", response.errorBody()?.string())
                    CodeResponse.ErrorData(
                        response.code(),
                        response.errorBody().toString(),
                        response.message()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Reject friend request failed with exception")
            CodeResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    companion object {
        private const val TAG = "FRIEND_REPO"
    }

}
