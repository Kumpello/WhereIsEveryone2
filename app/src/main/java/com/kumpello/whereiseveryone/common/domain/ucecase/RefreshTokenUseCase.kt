package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.model.AuthResponse

class RefreshTokenUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val getCurrentRefreshToken: GetCurrentRefreshTokenUseCase,
    private val saveKeyUseCase: SaveKeyUseCase,
) {

    suspend fun execute(): Response {
        val refreshToken = getCurrentRefreshToken.execute()
        if (refreshToken.isNullOrEmpty()) return Response.Error

        val response = authenticationRepository.refreshToken(refreshToken)

        if (response is AuthResponse.AuthData) {
            saveUserData(response)
        }
        return when (response) {
            is AuthResponse.AuthData -> Response.Success
            is AuthResponse.ErrorData -> Response.Error
        }
    }

    private suspend fun saveUserData(response: AuthResponse.AuthData): Response {
        saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY, response.token)
        saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY, response.refresh_token)

        return Response.Success
    }

    sealed class Response {
        data object Success : Response()
        data object Error : Response()
    }
}
