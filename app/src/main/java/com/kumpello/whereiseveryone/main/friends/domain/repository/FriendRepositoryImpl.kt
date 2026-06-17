package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.api.FriendApi
import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import timber.log.Timber

class FriendRepositoryImpl(
    private val friendApi: FriendApi
) : FriendRepository {

    override suspend fun addFriend(token: String, username: String): CodeResponse {
        val response = friendApi.addFriend("Bearer $token", FriendRequest(username))

        return when {
            response.isSuccessful -> CodeResponse.SuccessNoContent

            else -> {
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

    override suspend fun removeFriend(token: String, username: String): CodeResponse {
        val response = friendApi.removeFriend("Bearer $token", FriendRequest(username))

        return when {
            response.isSuccessful -> CodeResponse.SuccessNoContent

            else -> {
                Timber.e(response.errorBody().toString())
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

    override suspend fun acceptFriendRequest(
        token: String,
        username: String
    ): CodeResponse {
        val response =
            friendApi.acceptFriendRequest("Bearer $token", FriendRequest(username))

        return when {
            response.isSuccessful -> CodeResponse.SuccessNoContent

            else -> {
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

    override suspend fun rejectFriendRequest(
        token: String,
        username: String
    ): CodeResponse {
        val response =
            friendApi.rejectFriendRequest("Bearer $token", FriendRequest(username))

        return when {
            response.isSuccessful -> CodeResponse.SuccessNoContent

            else -> {
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

}