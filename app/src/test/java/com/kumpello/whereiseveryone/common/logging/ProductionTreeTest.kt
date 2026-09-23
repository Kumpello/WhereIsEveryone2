package com.kumpello.whereiseveryone.common.logging

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.io.IOException

class ProductionTreeTest {
    private data class Entry(val priority: Int, val tag: String?, val message: String)
    private val entries = mutableListOf<Entry>()
    private val tree = ProductionTree()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.println(any(), any(), any()) } answers {
            entries += Entry(firstArg(), secondArg(), thirdArg())
            0
        }
        Timber.plant(tree)
    }

    @After
    fun tearDown() {
        Timber.uproot(tree)
        unmockkStatic(Log::class)
    }

    @Test
    fun `release drops verbose debug and info before formatting arguments`() {
        val argument = object {
            override fun toString(): String = error("Filtered arguments must not be formatted")
        }
        Timber.v("%s", argument)
        Timber.d("%s", argument)
        Timber.i("%s", argument)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `release preserves warnings errors assertions and explicit tags`() {
        Timber.tag("LOCATION").w("Update failed (HTTP %d)", 429)
        Timber.tag("AUTH").e("Refresh unavailable")
        Timber.wtf("Unexpected state")
        assertEquals(listOf(
            Entry(Log.WARN, "LOCATION", "Update failed (HTTP 429)"),
            Entry(Log.ERROR, "AUTH", "Refresh unavailable"),
            Entry(Log.ASSERT, "WhereIsEveryone", "Unexpected state")
        ), entries)
    }

    @Test
    fun `release removes exception messages causes suppressed exceptions and stack traces`() {
        val exception = IOException("secret token\nprivate URL", IllegalStateException("private location"))
        exception.addSuppressed(IllegalArgumentException("private username"))
        Timber.tag("HTTP").w(exception, "Request failed after %d attempts", 3)
        Timber.e(exception)
        assertEquals(listOf(
            Entry(Log.WARN, "HTTP", "Request failed after 3 attempts [IOException]"),
            Entry(Log.ERROR, "WhereIsEveryone", "[IOException]")
        ), entries)
    }

    @Test
    fun `cancellation does not produce a production failure log`() {
        Timber.e(CancellationException("Screen closed"), "Request stopped")
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `filtered calls consume their tag`() {
        Timber.tag("DEBUG_ONLY").d("Filtered")
        Timber.w("Warning")
        assertEquals("WhereIsEveryone", entries.single().tag)
    }

    @Test
    fun `HTTP client failures are warnings and server failures are errors`() {
        Timber.tag("AUTH").httpFailure("Login", 401)
        Timber.tag("HTTP").httpFailure("Fetch friends", 429)
        Timber.tag("HTTP").httpFailure("Fetch friends", 503)
        assertEquals(listOf(
            Entry(Log.WARN, "AUTH", "Login failed (HTTP 401)"),
            Entry(Log.WARN, "HTTP", "Fetch friends failed (HTTP 429)"),
            Entry(Log.ERROR, "HTTP", "Fetch friends failed (HTTP 503)")
        ), entries)
    }
}
