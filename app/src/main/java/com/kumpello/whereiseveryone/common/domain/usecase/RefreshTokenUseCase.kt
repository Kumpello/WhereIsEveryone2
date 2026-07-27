package com.kumpello.whereiseveryone.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.provider.DeviceIdProvider
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.model.AuthResponse

class RefreshTokenUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val preferencesManager: PreferencesManager,
    private val deviceIdProvider: DeviceIdProvider,
) {

    suspend fun execute(): Response {
        val refreshToken = preferencesManager.get(PreferencesKey.AuthRefreshToken)
        if (refreshToken.isNullOrEmpty()) return Response.Error

        val deviceToken = deviceIdProvider.getDeviceId()
        val response = authenticationRepository.refreshToken(refreshToken, deviceToken)

        if (response is AuthResponse.AuthData) {
            saveUserData(response)
        }
        return when (response) {
            is AuthResponse.AuthData -> Response.Success
            is AuthResponse.ErrorData -> Response.Error
        }
    }

    private suspend fun saveUserData(response: AuthResponse.AuthData): Response {
        preferencesManager.save(PreferencesKey.AuthToken, response.token)
        preferencesManager.save(PreferencesKey.AuthRefreshToken, response.refresh_token)

        return Response.Success
    }

    sealed class Response {
        data object Success : Response()
        data object Error : Response()
    }
}
