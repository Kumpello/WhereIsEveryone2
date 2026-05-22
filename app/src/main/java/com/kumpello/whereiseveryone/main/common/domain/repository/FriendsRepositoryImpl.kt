package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.main.common.domain.model.FriendsApi
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import timber.log.Timber

class FriendsRepositoryImpl(
    private val friendsApi: FriendsApi
) : FriendsRepository {

    override fun getFriends(token: String): FriendsResponse {
        val response = friendsApi.getFriends("Bearer $token").execute()
        return when {
            response.isSuccessful -> FriendsResponse.FriendsData(
                response.body() ?: emptyList()
            )

            else -> {
                Timber.e(response.errorBody().toString())
                FriendsResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

}