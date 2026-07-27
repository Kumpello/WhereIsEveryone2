package com.kumpello.whereiseveryone.common.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.AuthApi
import com.kumpello.whereiseveryone.common.model.AuthResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthenticationRepositoryImplTest {

    private val authApi: AuthApi = mockk()
    private val repository = AuthenticationRepositoryImpl(authApi)

    @Test
    fun `signUp success returns AuthData`() = runTest {
        val authData = AuthResponse.AuthData("1", "refresh", "token")
        coEvery { authApi.signUp(match { it.device_token == "device_id" }) } returns Response.success(authData)

        val result = repository.signUp("user", "pass", "device_id")

        assertEquals(authData, result)
    }

    @Test
    fun `signUp failure returns ErrorData`() = runTest {
        coEvery { authApi.signUp(any()) } returns Response.error(400, "Bad Request".toResponseBody(null))

        val result = repository.signUp("user", "pass", "device_id")

        assertTrue(result is AuthResponse.ErrorData)
        assertEquals(400, (result as AuthResponse.ErrorData).code)
    }

    @Test
    fun `logIn success returns AuthData`() = runTest {
        val authData = AuthResponse.AuthData("1", "refresh", "token")
        coEvery { authApi.login(match { it.device_token == "device_id" }) } returns Response.success(authData)

        val result = repository.logIn("user", "pass", "device_id")

        assertEquals(authData, result)
    }

    @Test
    fun `logIn failure returns ErrorData`() = runTest {
        coEvery { authApi.login(any()) } returns Response.error(401, "Unauthorized".toResponseBody(null))

        val result = repository.logIn("user", "pass", "device_id")

        assertTrue(result is AuthResponse.ErrorData)
        assertEquals(401, (result as AuthResponse.ErrorData).code)
    }

    @Test
    fun `refreshToken success returns AuthData`() = runTest {
        val authData = AuthResponse.AuthData("1", "refresh", "token")
        coEvery { authApi.refresh(match { it.device_token == "device_id" }) } returns Response.success(authData)

        val result = repository.refreshToken("refresh_token", "device_id")

        assertEquals(authData, result)
    }

    @Test
    fun `refreshToken failure returns ErrorData`() = runTest {
        coEvery { authApi.refresh(any()) } returns Response.error(401, "Unauthorized".toResponseBody(null))

        val result = repository.refreshToken("refresh_token", "device_id")

        assertTrue(result is AuthResponse.ErrorData)
        assertEquals(401, (result as AuthResponse.ErrorData).code)
    }
}
