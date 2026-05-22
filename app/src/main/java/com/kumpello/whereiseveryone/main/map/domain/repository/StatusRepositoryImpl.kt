package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.api.StatusApi
import com.kumpello.whereiseveryone.main.map.domain.model.StatusRequest
import timber.log.Timber

class StatusRepositoryImpl(
    private val statusApi: StatusApi
) : StatusRepository {

    override fun updateStatus(token: String, status: String): CodeResponse {
        val response = statusApi
            .updateStatus("Bearer $token", StatusRequest(status))
            .execute()

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