package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.main.common.domain.model.FriendsApi
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import timber.log.Timber

class FriendsRepositoryImpl(
    private val friendsApi: FriendsApi
) : FriendsRepository {

    override suspend fun getFriends(token: String): FriendsResponse {
        return try {
            val response = friendsApi.getFriends("Bearer $token")
            if (response.isSuccessful) {
                Timber.tag(TAG).d("Successfully fetched friends")
                FriendsResponse.FriendsData(
                    response.body() ?: emptyList()
                )
            } else {
                Timber.tag(TAG).e("Error fetching friends: %s", response.errorBody()?.string())
                FriendsResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error fetching friends with exception")
            FriendsResponse.ErrorData(-1, e.message ?: "Unknown error", "Exception")
        }
    }

    companion object {
        private const val TAG = "FRIENDS_REPO"
    }

}