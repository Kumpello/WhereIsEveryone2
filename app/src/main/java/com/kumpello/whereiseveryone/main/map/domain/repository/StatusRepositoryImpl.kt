package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.api.StatusApi
import com.kumpello.whereiseveryone.main.map.domain.model.StatusRequest
import timber.log.Timber

class StatusRepositoryImpl(
    private val statusApi: StatusApi
) : StatusRepository {

    override suspend fun updateStatus(token: String, status: String): CodeResponse {
        val response = statusApi
            .updateStatus("Bearer $token", StatusRequest(status))

        return when {
            response.isSuccessful -> {
                Timber.tag(TAG).d("Status update successful")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).e("Status update failed: %s", response.errorBody()?.string())
                CodeResponse.ErrorData(
                    response.code(),
                    response.errorBody().toString(),
                    response.message()
                )
            }
        }
    }

    companion object {
        private const val TAG = "STATUS_REPO"
    }

}