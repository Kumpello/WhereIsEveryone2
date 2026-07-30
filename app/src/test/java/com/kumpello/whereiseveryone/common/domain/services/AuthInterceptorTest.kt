package com.kumpello.whereiseveryone.common.domain.services

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.usecase.RefreshTokenUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class AuthInterceptorTest {

    private val preferencesManager: PreferencesManager = mockk()
    private val refreshTokenUseCase: RefreshTokenUseCase = mockk()
    private lateinit var interceptor: AuthInterceptor

    @Before
    fun setup() {
        stopKoin()
        startKoin {
            modules(module {
                single { refreshTokenUseCase }
            })
        }
        interceptor = AuthInterceptor(preferencesManager)
    }

    @Test
    fun `intercept adds Authorization header when token exists`() {
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "test_token"
        
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
}
