package com.kumpello.whereiseveryone.app.logging

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.io.IOException

class ProductionTreeTest {
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
    fun `startup entries drain once and subsequent failures forward immediately`() {
        val reported = mutableListOf<String>()
        Timber.tag("Startup").w("Database unavailable")
        tree.attachReporter { component, message, _ -> reported += "$component: $message" }
        Timber.tag("Location").e("Acquisition unavailable")
        assertEquals(listOf("Startup: Database unavailable", "Location: Acquisition unavailable"), reported)

        tree.attachReporter { _, message, _ -> reported += message }
        assertEquals(2, reported.size)
    }

    @Test
    fun `startup buffer is bounded and retains the newest failures`() {
        repeat(150) { Timber.w("Failure %d", it) }
        val reported = mutableListOf<String>()
        tree.attachReporter { _, message, _ -> reported += message }
        assertEquals(100, reported.size)
        assertEquals("Failure 50", reported.first())
        assertEquals("Failure 149", reported.last())
    }

    @Test
    fun `remote forwarding receives sanitized context and throwable for safe metadata extraction`() {
        val exception = IOException("private token", IllegalStateException("private location"))
        var reportedComponent: String? = null
        var reportedMessage: String? = null
        var reportedThrowable: Throwable? = null
        tree.attachReporter { component, message, throwable ->
            reportedComponent = component
            reportedMessage = message
            reportedThrowable = throwable
        }
        Timber.tag("HTTP").w(exception, "Request unavailable")
        assertEquals("HTTP", reportedComponent)
        assertEquals("Request unavailable [IOException]", reportedMessage)
        assertSame(exception, reportedThrowable)
    }

    @Test
    fun `cancellation and filtered levels are never forwarded or buffered`() {
        Timber.w(CancellationException("Screen stopped"), "Request stopped")
        Timber.d("Debug detail")
        Timber.i("Information")
        val reported = mutableListOf<String>()
        tree.attachReporter { _, message, _ -> reported += message }
        Timber.e(CancellationException("Screen closed"), "Request stopped")
        assertTrue(reported.isEmpty())
    }

    @Test
    fun `reporter failure never escapes into the application`() {
        var attempts = 0
        tree.attachReporter { _, _, _ ->
            attempts++
            throw IllegalStateException("Unavailable diagnostics")
        }
        Timber.w("First warning")
        Timber.e("Second failure")
        assertEquals(2, attempts)
    }

    private data class Entry(val priority: Int, val tag: String?, val message: String)
}
