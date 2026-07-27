package com.kumpello.whereiseveryone.authentication.signUp.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.provider.DeviceIdProvider
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.model.AuthResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SignUpUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val deviceIdProvider: DeviceIdProvider = mockk()
    private lateinit var signUpUseCase: SignUpUseCase

    @Before
    fun setup() {
        signUpUseCase = SignUpUseCase(authenticationRepository, preferencesManager, deviceIdProvider)
        coEvery { deviceIdProvider.getDeviceId() } returns "device_id"
    }

    @Test
    fun `execute success saves data and returns Success`() = runTest {
        val authData = AuthResponse.AuthData(
            id = "1",
            refresh_token = "refresh",
            token = "token",
        )
        coEvery { authenticationRepository.signUp("user", "pass", "device_id") } returns authData

        val result = signUpUseCase.execute("user", "pass")

        assertEquals(SignUpUseCase.Response.Success, result)
        coVerify {
            preferencesManager.save(PreferencesKey.AuthToken, "token")
            preferencesManager.save(PreferencesKey.AuthRefreshToken, "refresh")
            preferencesManager.save(PreferencesKey.UserName, "user")
        }
    }

    @Test
    fun `execute failure returns Error`() = runTest {
        coEvery { authenticationRepository.signUp("user", "pass", "device_id") } returns AuthResponse.ErrorData(400, "Error", "Bad Request")

        val result = signUpUseCase.execute("user", "pass")

        assertEquals(SignUpUseCase.Response.Error, result)
        coVerify(exactly = 0) {
            preferencesManager.save(any<PreferencesKey<String>>(), any())
        }
    }
}
