package com.kumpello.whereiseveryone.authentication.login.domain.usecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.entity.AuthResponseWithParams
import com.kumpello.whereiseveryone.common.entity.Response
import com.kumpello.whereiseveryone.common.model.AuthResponse

class LoginUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val saveKeyUseCase: SaveKeyUseCase,
) {

    fun execute(
        username: String,
        password: String
    ): Response {
        val response = login(
            username = username,
            password = password
        )
        saveUserData(response)
        return when (response.authResponse) {
            is AuthResponse.AuthData -> Response.Success
            is AuthResponse.ErrorData -> Response.Error
        }
    }

    private fun login(username: String, password: String): AuthResponseWithParams {
        return AuthResponseWithParams(
            username = username,
            password = password,
            authResponse = authenticationRepository.logIn(
                username,
                password
            )
        )
    }

    private fun saveUserData(response: AuthResponseWithParams): Response {
        return when (response.authResponse) {
            is AuthResponse.AuthData -> {
                saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_ID_KEY, response.authResponse.id)
                saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_NAME_KEY, response.username)
                saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY, response.authResponse.token)
                saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY, response.authResponse.refresh_token)

                Response.Success
            }

            is AuthResponse.ErrorData -> {

                Response.Error
            }
        }
    }
}
