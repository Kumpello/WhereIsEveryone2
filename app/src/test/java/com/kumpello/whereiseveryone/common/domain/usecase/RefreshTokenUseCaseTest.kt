package com.kumpello.whereiseveryone.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.provider.DeviceIdProvider
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.model.AuthResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshTokenUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val deviceIdProvider: DeviceIdProvider = mockk()

    private val useCase = RefreshTokenUseCase(
        authenticationRepository,
        preferencesManager,
        deviceIdProvider
    )

    @Test
    fun `execute returns Error when refresh token is null or empty`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthRefreshToken) } returns null

        val result = useCase.execute()

        assertEquals(RefreshTokenUseCase.Response.Error, result)
    }

    @Test
    fun `execute success saves new tokens and returns Success`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthRefreshToken) } returns "old_refresh_token"
        coEvery { deviceIdProvider.getDeviceId() } returns "device_123"
        val authData = AuthResponse.AuthData(id = "1", refresh_token = "new_refresh_token", token = "new_access_token")
        coEvery { authenticationRepository.refreshToken("old_refresh_token", "device_123") } returns authData

        val result = useCase.execute()

        assertEquals(RefreshTokenUseCase.Response.Success, result)
        coVerify {
            preferencesManager.save(PreferencesKey.AuthToken, "new_access_token")
            preferencesManager.save(PreferencesKey.AuthRefreshToken, "new_refresh_token")
        }
    }

    @Test
    fun `execute error returns Error without saving tokens`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthRefreshToken) } returns "old_refresh_token"
        coEvery { deviceIdProvider.getDeviceId() } returns "device_123"
        coEvery { authenticationRepository.refreshToken("old_refresh_token", "device_123") } returns AuthResponse.ErrorData(401, "Error", "Unauthorized")

        val result = useCase.execute(maxRetries = 3, initialDelayMs = 0L)

        assertEquals(RefreshTokenUseCase.Response.Error, result)
        coVerify(exactly = 0) {
            preferencesManager.save(any<PreferencesKey<String>>(), any())
        }
    }

    @Test
    fun `execute network exception retries and returns NetworkError when all attempts fail`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthRefreshToken) } returns "old_refresh_token"
        coEvery { deviceIdProvider.getDeviceId() } returns "device_123"
        coEvery { authenticationRepository.refreshToken("old_refresh_token", "device_123") } throws java.io.IOException("ConnectException: ECONNABORTED")

        val result = useCase.execute(maxRetries = 3, initialDelayMs = 0L)

        assertEquals(RefreshTokenUseCase.Response.NetworkError, result)
        coVerify(exactly = 3) {
            authenticationRepository.refreshToken("old_refresh_token", "device_123")
        }
        coVerify(exactly = 0) {
            preferencesManager.save(any<PreferencesKey<String>>(), any())
        }
    }

    @Test
    fun `execute network exception retries and returns Success when subsequent attempt succeeds`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthRefreshToken) } returns "old_refresh_token"
        coEvery { deviceIdProvider.getDeviceId() } returns "device_123"
        val authData = AuthResponse.AuthData(id = "1", refresh_token = "new_refresh_token", token = "new_access_token")
        coEvery { authenticationRepository.refreshToken("old_refresh_token", "device_123") } throws java.io.IOException("ConnectException") andThen authData

        val result = useCase.execute(maxRetries = 3, initialDelayMs = 0L)

        assertEquals(RefreshTokenUseCase.Response.Success, result)
        coVerify(exactly = 2) {
            authenticationRepository.refreshToken("old_refresh_token", "device_123")
        }
        coVerify {
            preferencesManager.save(PreferencesKey.AuthToken, "new_access_token")
            preferencesManager.save(PreferencesKey.AuthRefreshToken, "new_refresh_token")
        }
    }
}
