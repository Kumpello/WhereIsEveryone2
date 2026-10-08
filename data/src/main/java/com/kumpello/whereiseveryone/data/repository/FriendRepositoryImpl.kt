package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.network.api.FriendApi
import com.kumpello.whereiseveryone.data.network.model.FriendRequest
import com.kumpello.whereiseveryone.data.logging.httpFailure
import timber.log.Timber

internal class FriendRepositoryImpl(
    private val friendApi: FriendApi
) : FriendRepository {

    override suspend fun addFriend(username: String): CodeResponse {
        val response = friendApi.addFriend(FriendRequest(username))

        return when {
            response.isSuccessful -> {
                Timber.tag(TAG).d("Add friend request successful")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).httpFailure("Add friend request", response.code())
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
                Timber.tag(TAG).d("Remove friend successful")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).httpFailure("Remove friend", response.code())
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
                Timber.tag(TAG).d("Accept friend request successful")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).httpFailure("Accept friend request", response.code())
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
                Timber.tag(TAG).d("Reject friend request successful")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).httpFailure("Reject friend request", response.code())
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
