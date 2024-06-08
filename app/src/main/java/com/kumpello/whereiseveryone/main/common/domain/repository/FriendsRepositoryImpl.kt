package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient

import com.kumpello.whereiseveryone.main.common.domain.model.FriendsApi
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import timber.log.Timber

class FriendsRepositoryImpl : FriendsRepository {

    private val retrofit = RetrofitClient.getClient()
    private val friendsApi = retrofit.create(FriendsApi::class.java)

    override fun getPositions(token: String): PositionsResponse {
        val response = friendsApi.getPositions("Bearer $token").execute()
        return when {
            response.isSuccessful -> response.body() ?: PositionsResponse.FriendsData(
                emptyList()
            )

            else -> {
                Timber.e(response.errorBody().toString())
                PositionsResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

}