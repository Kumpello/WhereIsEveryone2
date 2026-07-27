package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.api.FriendApi
import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import timber.log.Timber

class FriendRepositoryImpl(
    private val friendApi: FriendApi
) : FriendRepository {

    override suspend fun addFriend(username: String): CodeResponse {
        val response = friendApi.addFriend(FriendRequest(username))

        return when {
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
    }

    override suspend fun removeFriend(username: String): CodeResponse {
        val response = friendApi.removeFriend(FriendRequest(username))

        return when {
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
    }

    override suspend fun acceptFriendRequest(
        username: String
    ): CodeResponse {
        val response =
            friendApi.acceptFriendRequest(FriendRequest(username))

        return when {
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
    }

    override suspend fun rejectFriendRequest(
        username: String
    ): CodeResponse {
        val response =
            friendApi.rejectFriendRequest(FriendRequest(username))

        return when {
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
    }

    companion object {
        private const val TAG = "FRIEND_REPO"
    }

}
