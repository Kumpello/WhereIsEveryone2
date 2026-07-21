package com.kumpello.whereiseveryone.authentication.login.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.model.AuthResponse

class LoginUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val preferencesManager: PreferencesManager,
) {

    suspend fun execute(
        username: String,
        password: String
    ): Response {
        val response = login(
            username = username,
            password = password
        )
        if (response.authResponse is AuthResponse.AuthData) {
            saveUserData(response)
        }
        return when (response.authResponse) {
            is AuthResponse.AuthData -> Response.Success
            is AuthResponse.ErrorData -> Response.Error
        }
    }

    private suspend fun login(username: String, password: String): AuthResponseWithParams {
        return AuthResponseWithParams(
            username = username,
            password = password,
            authResponse = authenticationRepository.logIn(
                username,
                password
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
