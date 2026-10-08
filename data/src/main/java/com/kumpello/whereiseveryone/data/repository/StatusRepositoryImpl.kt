package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.network.api.StatusApi
import com.kumpello.whereiseveryone.data.network.model.StatusRequest
import com.kumpello.whereiseveryone.data.logging.httpFailure
import timber.log.Timber

internal class StatusRepositoryImpl(
    private val statusApi: StatusApi
) : StatusRepository {

    override suspend fun updateStatus(status: String): CodeResponse {
        val response = statusApi
            .updateStatus(StatusRequest(status))

        return when {
            response.isSuccessful -> {
                Timber.tag(TAG).d("Status update successful")
                CodeResponse.SuccessNoContent
            }

            else -> {
                Timber.tag(TAG).httpFailure("Status update", response.code())
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