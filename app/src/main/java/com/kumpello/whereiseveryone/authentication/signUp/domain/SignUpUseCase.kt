package com.kumpello.whereiseveryone.authentication.domain

import com.kumpello.whereiseveryone.common.data.model.authorization.AuthResponse
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationService
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.entities.AuthResponseWithParams
import com.kumpello.whereiseveryone.common.entities.Response

class SignUpUseCase(
    private val authenticationService: AuthenticationService,
    private val saveKeyUseCase: SaveKeyUseCase,
) {

    fun execute(
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

    private fun signUp(username: String, password: String): AuthResponseWithParams {
        return AuthResponseWithParams(
            username = username,
            password = password,
            authResponse = authenticationService.signUp(
                username,
                password
            ))
    }

    private fun saveUserData(response: AuthResponseWithParams) : Response {
        return when (response.authResponse) {
            is AuthResponse.AuthData -> {
                saveKeyUseCase.saveUserID(response.authResponse.id)
                saveKeyUseCase.saveUserName(response.username)
                saveKeyUseCase.saveAuthToken(response.authResponse.token)
                saveKeyUseCase.saveAuthRefreshToken(response.authResponse.refresh_token)

                Response.Success
            }

            is AuthResponse.ErrorData -> {

                Response.Error
            }
        }
    }
}
