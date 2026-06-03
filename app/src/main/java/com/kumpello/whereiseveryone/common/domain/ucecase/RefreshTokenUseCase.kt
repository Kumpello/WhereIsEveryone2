package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.entity.Response
import com.kumpello.whereiseveryone.common.model.AuthResponse

class RefreshTokenUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val getCurrentRefreshToken: GetCurrentRefreshTokenUseCase,
    private val saveKeyUseCase: SaveKeyUseCase,
) {

    fun execute(): Response {
        val refreshToken = getCurrentRefreshToken.execute()
        if (refreshToken.isNullOrEmpty()) return Response.Error

        val response = authenticationRepository.refreshToken(refreshToken)

        saveUserData(response)
        return when (response) {
            is AuthResponse.AuthData -> Response.Success
            is AuthResponse.ErrorData -> Response.Error
        }
    }

    private fun saveUserData(response: AuthResponse): Response {
        return when (response) {
            is AuthResponse.AuthData -> {
                saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_ID_KEY, response.id)
                saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY, response.token)
                saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY, response.refresh_token)

                Response.Success
            }

            is AuthResponse.ErrorData -> {
                Response.Error
            }
        }
    }
}
