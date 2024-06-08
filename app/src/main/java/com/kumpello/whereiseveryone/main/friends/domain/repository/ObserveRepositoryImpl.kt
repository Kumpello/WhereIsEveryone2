package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient
import com.kumpello.whereiseveryone.main.friends.domain.model.ObserveApi
import timber.log.Timber

class ObserveRepositoryImpl : ObserveRepository {

    private val retrofit = RetrofitClient.getClient()
    private val observeApi = retrofit.create(ObserveApi::class.java)

    override fun addFriend(token: String, nick: String): CodeResponse {
        val response = observeApi.addFriend("Bearer $token", nick).execute()

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

    override fun removeFriend(token: String, nick: String): CodeResponse {
        val response = observeApi.removeFriend("Bearer $token", nick).execute()

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

}