package com.kumpello.whereiseveryone.authentication.login.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.model.AuthResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val loginUseCase = LoginUseCase(authenticationRepository, preferencesManager)

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
            preferencesManager.save(PreferencesKey.AuthToken, "token")
            preferencesManager.save(PreferencesKey.AuthRefreshToken, "refresh")
            preferencesManager.save(PreferencesKey.UserName, "user")
        }
    }

    @Test
    fun `execute failure returns Error`() = runTest {
        coEvery { authenticationRepository.logIn("user", "pass") } returns AuthResponse.ErrorData(401, "Error", "Unauthorized")

        val result = loginUseCase.execute("user", "pass")

        assertEquals(LoginUseCase.Response.Error, result)
        coVerify(exactly = 0) {
            preferencesManager.save(any<PreferencesKey<String>>(), any())
        }
    }
}
