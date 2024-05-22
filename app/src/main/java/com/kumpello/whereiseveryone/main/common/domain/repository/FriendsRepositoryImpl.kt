package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient
import com.kumpello.whereiseveryone.common.model.ErrorData
import com.kumpello.whereiseveryone.main.common.domain.model.FriendsApi
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import timber.log.Timber

class FriendsRepositoryImpl : FriendsRepository {

    private val retrofit = RetrofitClient.getClient()
    private val friendsApi = retrofit.create(FriendsApi::class.java)

    override fun getPositions(token: String): PositionsResponse {
        val response = friendsApi.getPositions("Bearer: $token").execute()
        return if (response.isSuccessful) {
            response.body()!!
        } else {
            Timber.e(response.errorBody().toString())
            ErrorData(response.code(), response.errorBody().toString(), response.message())
        }
    }

}