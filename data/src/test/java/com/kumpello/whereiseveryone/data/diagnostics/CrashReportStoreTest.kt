package com.kumpello.whereiseveryone.data.diagnostics

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class CrashReportStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `crash survives restart with stable ID and safe application frames only`() {
        val file = File(temporaryFolder.root, "crash.json")
        val store = CrashReportStore(file, "0.9")
        val error = IOException("private token", IllegalStateException("private location")).apply {
            stackTrace = arrayOf(
                StackTraceElement("com.kumpello.whereiseveryone.Sample", "load", "Sample.kt", 12),
                StackTraceElement("java.io.File", "open", "File.java", 3),
            )
        }
        store.save(error, "car", "0123456789abcdef01234567")
        val restored = CrashReportStore(file, "0.9").read()!!

        assertEquals("fatal", restored.event.type)
        assertEquals("car", restored.event.trackingMode)
        assertNotNull(restored.event.eventId)
        assertEquals(restored.event.eventId, store.read()!!.event.eventId)
        assertTrue(restored.event.stackTrace!!.contains("Sample.load"))
        assertFalse(file.readText().contains("private"))
        assertFalse(file.readText().contains("java.io.File"))
        store.clear(restored.event.eventId)
        assertFalse(file.exists())
    }

    @Test
    fun `clearing an imported event preserves a newer journal`() {
        val store = CrashReportStore(temporaryFolder.newFile(), "0.9")
        store.save(IOException("old"))
        val oldId = store.read()!!.event.eventId
        store.save(IOException("new"))
        val newId = store.read()!!.event.eventId

        store.clear(oldId)
        assertEquals(newId, store.read()!!.event.eventId)
        store.clear(newId)
        assertNull(store.read())
    }

    @Test
    fun `corrupt or oversized journal is dropped without blocking startup`() {
        val file = temporaryFolder.newFile()
        val store = CrashReportStore(file, "0.9")
        file.writeText("not json")
        assertNull(store.read())
        file.writeText("x".repeat(16_385))
        assertNull(store.read())
        assertFalse(file.exists())
    }

    @Test
    fun `cancellation is not persisted as a crash`() {
        val file = File(temporaryFolder.root, "crash.json")
        CrashReportStore(file, "0.9").save(CancellationException())
        assertFalse(file.exists())
    }

    @Test
    fun `diagnostics redact URLs credentials emails and precise numbers`() {
        val safe = safeDiagnosticMessage("Failed https://host/secret Bearer abc user@example.com 52.123456\nprivate cause")
        assertEquals("Failed [url] [credential] [email] [number]", safe)
    }
}
