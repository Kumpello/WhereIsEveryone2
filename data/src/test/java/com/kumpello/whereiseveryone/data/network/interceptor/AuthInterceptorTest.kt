package com.kumpello.whereiseveryone.data.network.interceptor

import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.session.RefreshTokenUseCase
import com.kumpello.whereiseveryone.data.session.LogoutUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class AuthInterceptorTest {

    private val preferencesManager: PreferencesManager = mockk()
    private val refreshTokenUseCase: RefreshTokenUseCase = mockk()
    private val logoutUseCase: LogoutUseCase = mockk(relaxed = true)
    private lateinit var interceptor: AuthInterceptor

    @Before
    fun setup() {
        stopKoin()
        startKoin {
            modules(module {
                single { refreshTokenUseCase }
                single { logoutUseCase }
            })
        }
        interceptor = AuthInterceptor(preferencesManager)
    }

    @Test
    fun `intercept adds Authorization header when token exists`() {
        every { preferencesManager.getCached(PreferencesKey.AuthToken) } returns "test_token"
        
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url("http://api.com/data").build()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val interceptedRequest = it.invocation.args[0] as Request
            assertEquals("Bearer test_token", interceptedRequest.header("Authorization"))
            Response.Builder()
                .request(interceptedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        interceptor.intercept(chain)
    }

    @Test
    fun `intercept ignores auth requests`() {
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url("http://api.com/auth/login").build()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val interceptedRequest = it.invocation.args[0] as Request
            assertEquals(null, interceptedRequest.header("Authorization"))
            Response.Builder()
                .request(interceptedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        interceptor.intercept(chain)
    }

    @Test
    fun `server session invalidation clears only the rejected access session`() {
        every { preferencesManager.getCached(PreferencesKey.AuthToken) } returns "access"
        interceptor.intercept(chainReturningForbidden("invalid session"))
        coVerify(exactly = 1) { logoutUseCase.execute(expectedAccessToken = "access") }
    }

    @Test
    fun `resource denial does not delete session or restore key`() {
        every { preferencesManager.getCached(PreferencesKey.AuthToken) } returns "access"
        interceptor.intercept(chainReturningForbidden("forbidden"))
        coVerify(exactly = 0) { logoutUseCase.execute(any(), any()) }
    }

    private fun chainReturningForbidden(body: String): Interceptor.Chain {
        val request = Request.Builder().url("https://api.example/me/probe").build()
        return mockk {
            every { request() } returns request
            every { proceed(any()) } answers {
                Response.Builder().request(firstArg()).protocol(Protocol.HTTP_1_1)
                    .code(403).message("Forbidden").body(body.toResponseBody()).build()
            }
        }
    }
}
