package com.kumpello.whereiseveryone.authentication.signUp.domain.usecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.model.AuthResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SignUpUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk()
    private val saveKeyUseCase: SaveKeyUseCase = mockk(relaxed = true)
    private val signUpUseCase = SignUpUseCase(authenticationRepository, saveKeyUseCase)

    @Test
    fun `execute success saves data and returns Success`() = runTest {
        val authData = AuthResponse.AuthData(
            id = "1",
            refresh_token = "refresh",
            token = "token"
        )
        coEvery { authenticationRepository.signUp("user", "pass") } returns authData

        val result = signUpUseCase.execute("user", "pass")

        assertEquals(SignUpUseCase.Response.Success, result)
        coVerify {
            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY, "token")
            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY, "refresh")
            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_NAME_KEY, "user")
        }
    }

    @Test
    fun `execute failure returns Error`() = runTest {
        coEvery { authenticationRepository.signUp("user", "pass") } returns AuthResponse.ErrorData(400, "Error", "Bad Request")

        val result = signUpUseCase.execute("user", "pass")

        assertEquals(SignUpUseCase.Response.Error, result)
        coVerify(exactly = 0) {
            saveKeyUseCase.saveValue(any(), any())
        }
    }
}
