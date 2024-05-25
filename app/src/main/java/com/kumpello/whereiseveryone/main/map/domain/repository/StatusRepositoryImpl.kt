package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.SuccessResponse
import com.kumpello.whereiseveryone.common.domain.services.RetrofitClient
import com.kumpello.whereiseveryone.common.model.ErrorData
import com.kumpello.whereiseveryone.main.map.domain.model.StatusApi
import timber.log.Timber

class StatusRepositoryImpl : StatusRepository {

    private val retrofit = RetrofitClient.getClient()
    private val statusApi = retrofit.create(StatusApi::class.java)

    override fun updateStatus(token: String, message: String): SuccessResponse {
        val response = statusApi.updateStatus("Bearer: $token", message).execute()
        return if (response.isSuccessful) {
            SuccessResponse.SuccessData(response.code())
        } else {
            Timber.e(response.errorBody().toString())
            ErrorData(response.code(), response.errorBody().toString(), response.message())
        }
    }

}