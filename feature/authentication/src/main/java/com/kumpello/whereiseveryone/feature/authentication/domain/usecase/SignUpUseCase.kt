package com.kumpello.whereiseveryone.feature.authentication.domain.usecase

import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.device.DeviceIdProvider
import com.kumpello.whereiseveryone.data.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.data.network.model.AuthResponse

class SignUpUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val preferencesManager: PreferencesManager,
    private val deviceIdProvider: DeviceIdProvider,
) {

    suspend fun execute(
        username: String,
        password: String
    ) : Response {
        val response = signUp(
            username = username,
            password = password
        )
        if (response.authResponse is AuthResponse.AuthData) {
            saveUserData(response)
        }
        return when(response.authResponse) {
            is AuthResponse.AuthData -> Response.Success
            is AuthResponse.ErrorData -> Response.Error
        }
    }

    private suspend fun signUp(username: String, password: String): AuthResponseWithParams {
        val deviceToken = deviceIdProvider.getDeviceId()
        return AuthResponseWithParams(
            username = username,
            password = password,
            authResponse = authenticationRepository.signUp(
                username,
                password,
                deviceToken
            )
        )
    }

    private suspend fun saveUserData(responseWithParams: AuthResponseWithParams) {
        if (responseWithParams.authResponse is AuthResponse.AuthData) {
            preferencesManager.save(PreferencesKey.AuthToken, responseWithParams.authResponse.token)
            preferencesManager.save(PreferencesKey.AuthRefreshToken, responseWithParams.authResponse.refresh_token)
            preferencesManager.save(PreferencesKey.UserName, responseWithParams.username)
        }
    }

    sealed class Response {
        data object Success : Response()
        data object Error : Response()
    }

    private data class AuthResponseWithParams(
        val username: String,
        val password: String,
        val authResponse: AuthResponse
    )
}
