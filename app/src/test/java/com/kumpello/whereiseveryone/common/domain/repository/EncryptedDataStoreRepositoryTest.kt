package com.kumpello.whereiseveryone.common.domain.repository

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

class EncryptedDataStoreRepositoryTest {
    private val context = mockk<Context>()
    private val aead = mockk<Aead>()

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), Base64.NO_WRAP) } returns "encoded"
        every { Base64.decode("encoded", Base64.NO_WRAP) } returns byteArrayOf(1)
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `keyset initialization encryption and decryption run on IO dispatcher`() = runTest {
        Executors.newSingleThreadExecutor { Thread(it, "crypto-io") }.asCoroutineDispatcher().use { io ->
            val threads = mutableListOf<String>()
            val repository = EncryptedDataStoreRepository(context, io) {
                threads += Thread.currentThread().name
                aead
            }
            every { aead.encrypt(any(), any()) } answers {
                threads += Thread.currentThread().name
                byteArrayOf(1)
            }
            every { aead.decrypt(any(), any()) } answers {
                threads += Thread.currentThread().name
                "plaintext".toByteArray()
            }

            assertEquals("encoded", repository.encrypt("plaintext"))
            assertEquals("plaintext", repository.decrypt("encoded"))
            assertEquals(listOf("crypto-io", "crypto-io", "crypto-io"), threads)
        }
    }

    @Test
    fun `decrypt propagates cancellation instead of returning missing data`() = runTest {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { io ->
            val repository = EncryptedDataStoreRepository(context, io) { aead }
            val cancellation = CancellationException("Cancelled")
            every { aead.decrypt(any(), any()) } throws cancellation
            try {
                repository.decrypt("encoded")
                fail("Expected cancellation")
            } catch (actual: CancellationException) {
                assertEquals(cancellation.message, actual.message)
            }
        }
    }
}
