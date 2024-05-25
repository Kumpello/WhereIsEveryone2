package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.SuccessResponse
import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient
import com.kumpello.whereiseveryone.common.model.ErrorData
import com.kumpello.whereiseveryone.main.friends.domain.model.ObserveApi
import timber.log.Timber

class ObserveRepositoryImpl : ObserveRepository {

    private val retrofit = RetrofitClient.getClient()
    private val observeApi = retrofit.create(ObserveApi::class.java)

    override fun addFriend(token: String, nick: String): SuccessResponse {
        val response = observeApi.addFriend("Bearer: $token", nick).execute()
        return if (response.isSuccessful) {
            SuccessResponse.SuccessData(response.code())
        } else {
            Timber.e(response.errorBody().toString())
            ErrorData(response.code(), response.errorBody().toString(), response.message())
        }
    }

    override fun removeFriend(token: String, nick: String): SuccessResponse {
        val response = observeApi.removeFriend("Bearer: $token", nick).execute()
        return if (response.isSuccessful) {
            SuccessResponse.SuccessData(response.code())
        } else {
            Timber.e(response.errorBody().toString())
            ErrorData(response.code(), response.errorBody().toString(), response.message())
        }
    }

}