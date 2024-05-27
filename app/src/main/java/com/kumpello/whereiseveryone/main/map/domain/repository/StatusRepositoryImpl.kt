package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient

import com.kumpello.whereiseveryone.main.map.domain.model.StatusApi
import timber.log.Timber

class StatusRepositoryImpl : StatusRepository {

    private val retrofit = RetrofitClient.getClient()
    private val statusApi = retrofit.create(StatusApi::class.java)

    override fun updateStatus(token: String, message: String): CodeResponse {
        val response = statusApi.updateStatus("Bearer: $token", message).execute()

        return when {
            response.isSuccessful -> CodeResponse.SuccessData(response.code())

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