package com.kumpello.whereiseveryone.main.map.presentation

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.ProtocolException
import javax.net.ssl.SSLHandshakeException

@OptIn(ExperimentalCoroutinesApi::class)
class LocationUploadTest {
    private val upload = LocationUpload(mockk(), 123L)

    @Test
    fun `network failures retry three times with exponential delays and preserve timestamp`() = runTest {
        val attempts = mutableListOf<Pair<Long, LocationUpload>>()
        val result = sendLocationWithRetry(upload, { null }) {
            attempts += currentTime to it
            if (attempts.size < 3) throw IOException("Offline")
            CodeResponse.SuccessNoContent
        }

        assertSame(CodeResponse.SuccessNoContent, result)
        assertEquals(listOf(0L, 5_000L, 15_000L), attempts.map { it.first })
        assertEquals(listOf(upload, upload, upload), attempts.map { it.second })
    }

    @Test
    fun `exhausted network failures propagate after three attempts`() = runTest {
        var attempts = 0
        try {
            sendLocationWithRetry(upload, { null }) {
                attempts++
                throw IOException("Offline")
            }
            fail("Expected IOException")
        } catch (exception: IOException) {
            assertEquals("Offline", exception.message)
        }
        assertEquals(3, attempts)
        assertEquals(15_000L, currentTime)
    }

    @Test
    fun `transient HTTP responses are retried with a bounded budget`() = runTest {
        for (code in listOf(408, 500, 502, 503, 504)) {
            var attempts = 0
            val error = CodeResponse.ErrorData(code, "Unavailable", "Unavailable")
            val result = sendLocationWithRetry(upload, { null }) {
                attempts++
                error
            }
            assertSame(error, result)
            assertEquals(3, attempts)
        }
    }

    @Test
    fun `success and nonretryable HTTP responses return immediately`() = runTest {
        val responses = listOf(CodeResponse.SuccessNoContent) +
            listOf(400, 401, 403, 404, 409, 429, 501).map { CodeResponse.ErrorData(it, "Error", "Error") }
        for (response in responses) {
            var attempts = 0
            assertSame(response, sendLocationWithRetry(upload, { null }) {
                attempts++
                response
            })
            assertEquals(1, attempts)
        }
        assertEquals(0L, currentTime)
    }

    @Test
    fun `retry takes newest pending location without resetting failure budget`() = runTest {
        val pending = Channel<LocationUpload>(Channel.CONFLATED)
        val newer = LocationUpload(mockk(), 456L)
        val newest = LocationUpload(mockk(), 789L)
        val attempts = mutableListOf<LocationUpload>()
        val error = CodeResponse.ErrorData(503, "Unavailable", "Unavailable")

        val sender = launch {
            assertSame(error, sendLocationWithRetry(upload, { pending.tryReceive().getOrNull() }) {
                attempts += it
                error
            })
        }
        runCurrent()
        pending.send(newer)
        pending.send(newest)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(listOf(upload, newest), attempts)
        sender.join()
        assertEquals(listOf(upload, newest, newest), attempts)
        assertEquals(15_000L, currentTime)
    }

    @Test
    fun `cancellation during backoff prevents further uploads`() = runTest {
        var attempts = 0
        val sender = launch {
            sendLocationWithRetry(upload, { null }) {
                attempts++
                throw IOException("Offline")
            }
        }
        runCurrent()
        sender.cancel()
        sender.join()
        advanceTimeBy(60_000)
        assertEquals(1, attempts)
    }

    @Test
    fun `cancellation and permanent exceptions propagate without retry`() = runTest {
        for (failure in listOf(
            CancellationException("Cancelled"),
            IllegalStateException("Invalid state"),
            ProtocolException("Invalid response"),
            SSLHandshakeException("Invalid certificate")
        )) {
            var attempts = 0
            try {
                sendLocationWithRetry(upload, { null }) {
                    attempts++
                    throw failure
                }
                fail("Expected failure")
            } catch (exception: Exception) {
                assertEquals(failure.javaClass, exception.javaClass)
            }
            assertEquals(1, attempts)
        }
        assertEquals(0L, currentTime)
    }
}
