package com.kumpello.whereiseveryone.authentication.login.domain.usecase

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

class LoginUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk()
    private val saveKeyUseCase: SaveKeyUseCase = mockk(relaxed = true)
    private val loginUseCase = LoginUseCase(authenticationRepository, saveKeyUseCase)

    @Test
    fun `execute success saves data and returns Success`() = runTest {
        val authData = AuthResponse.AuthData(
            id = "1",
            refresh_token = "refresh",
            token = "token"
        )
        coEvery { authenticationRepository.logIn("user", "pass") } returns authData

        val result = loginUseCase.execute("user", "pass")

        assertEquals(LoginUseCase.Response.Success, result)
        coVerify {
            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY, "token")
            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY, "refresh")
            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_NAME_KEY, "user")
        }
    }

    @Test
    fun `execute failure returns Error`() = runTest {
        coEvery { authenticationRepository.logIn("user", "pass") } returns AuthResponse.ErrorData(401, "Error", "Unauthorized")

        val result = loginUseCase.execute("user", "pass")

        assertEquals(LoginUseCase.Response.Error, result)
        coVerify(exactly = 0) {
            saveKeyUseCase.saveValue(any(), any())
        }
    }
}
