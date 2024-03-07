package com.kumpello.whereiseveryone.authentication.signUp.domain.usecase

import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.entity.AuthResponseWithParams
import com.kumpello.whereiseveryone.common.entity.Response
import com.kumpello.whereiseveryone.common.model.AuthResponse

class SignUpUseCase(
    private val authenticationRepository: AuthenticationRepository,
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
            authResponse = authenticationRepository.signUp(
                username,
                password
            )
        )
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
