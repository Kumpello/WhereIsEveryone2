package com.kumpello.whereiseveryone.data.session

import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.device.DeviceIdProvider
import com.kumpello.whereiseveryone.data.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.data.network.model.AuthResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class RefreshTokenUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val preferencesManager: PreferencesManager,
    private val deviceIdProvider: DeviceIdProvider,
) {
    private val refreshMutex = Mutex()

    suspend fun execute(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS
    ): Response {
        val refreshToken = preferencesManager.get(PreferencesKey.AuthRefreshToken)
        if (refreshToken.isNullOrEmpty()) return Response.Error

        return refreshMutex.withLock {
            val currentRefreshToken = preferencesManager.get(PreferencesKey.AuthRefreshToken)
            when {
                currentRefreshToken.isNullOrEmpty() -> Response.Error
                // Another caller already rotated this single-use credential while we waited.
                currentRefreshToken != refreshToken -> Response.Success
                else -> refresh(refreshToken, maxRetries, initialDelayMs)
            }
        }
    }

    private suspend fun refresh(
        refreshToken: String,
        maxRetries: Int,
        initialDelayMs: Long
    ): Response {
        val deviceToken = deviceIdProvider.getDeviceId()

        var currentDelay = initialDelayMs
        for (attempt in 1..maxRetries) {
            try {
                val response = authenticationRepository.refreshToken(refreshToken, deviceToken)
                return if (response is AuthResponse.AuthData) {
                    saveUserData(response)
                    Response.Success
                } else {
                    Response.Error
                }
            } catch (e: IOException) {
                if (attempt == maxRetries) {
                    Timber.tag(TAG).w(e, "Token refresh failed after $maxRetries attempts due to network error")
                    return Response.NetworkError
                }
                Timber.tag(TAG).d(e, "Token refresh attempt $attempt failed. Retrying in ${currentDelay}ms...")
                if (currentDelay > 0) {
                    delay(currentDelay.milliseconds)
                }
                currentDelay *= 2
            }
        }

        return Response.NetworkError
    }

    private suspend fun saveUserData(response: AuthResponse.AuthData): Response {
        preferencesManager.save(PreferencesKey.AuthToken, response.token)
        preferencesManager.save(PreferencesKey.AuthRefreshToken, response.refresh_token)

        return Response.Success
    }

    sealed class Response {
        data object Success : Response()
        data object Error : Response()
        data object NetworkError : Response()
    }

    companion object {
        private const val TAG = "RefreshTokenUseCase"
        private const val DEFAULT_MAX_RETRIES = 3
        private const val DEFAULT_INITIAL_DELAY_MS = 1000L
    }
}
