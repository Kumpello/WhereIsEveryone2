package com.kumpello.whereiseveryone.authentication.signUp.domain.usecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.model.AuthResponse

class SignUpUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val saveKeyUseCase: SaveKeyUseCase,
) {

    suspend fun execute(
        username: String,
        password: String
    ) : Response {
        val response = signUp(
            username = username,
            password = password
        )
        saveUserData(response)
        return when(response.authResponse) {
            is AuthResponse.AuthData -> Response.Success
            is AuthResponse.ErrorData -> Response.Error
        }
    }

    private suspend fun signUp(username: String, password: String): AuthResponseWithParams {
        return AuthResponseWithParams(
            username = username,
            password = password,
            authResponse = authenticationRepository.signUp(
                username,
                password
            )
        )
    }

    private fun saveUserData(responseWithParams: AuthResponseWithParams) {
        if (responseWithParams.authResponse is AuthResponse.AuthData) {
            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY, responseWithParams.authResponse.token)
            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY, responseWithParams.authResponse.refresh_token)
            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_NAME_KEY, responseWithParams.username)
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
