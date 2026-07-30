package com.kumpello.whereiseveryone.common.domain.services

import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Test

class RequestInterceptorTest {

    @Test
    fun `intercept logs and proceeds`() {
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url("http://api.com").build()
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        RequestInterceptor.intercept(chain)
    }
}
