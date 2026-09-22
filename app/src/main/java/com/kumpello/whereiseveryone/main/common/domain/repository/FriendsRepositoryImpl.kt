package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.main.common.domain.model.FriendsApi
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import com.kumpello.whereiseveryone.common.logging.httpFailure
import timber.log.Timber

class FriendsRepositoryImpl(
    private val friendsApi: FriendsApi
) : FriendsRepository {

    override suspend fun getFriends(): FriendsResponse {
        val response = friendsApi.getFriends()
        return if (response.isSuccessful) {
            Timber.tag(TAG).d("Successfully fetched friends")
            FriendsResponse.FriendsData(
                response.body() ?: emptyList()
            )
        } else {
            Timber.tag(TAG).httpFailure("Fetch friends", response.code())
            FriendsResponse.ErrorData(
                response.code(),
                response.errorBody().toString(),
                response.message()
            )
        }
    }

    companion object {
        private const val TAG = "FRIENDS_REPO"
    }

}