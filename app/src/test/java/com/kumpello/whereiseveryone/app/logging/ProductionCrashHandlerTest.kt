package com.kumpello.whereiseveryone.app.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProductionCrashHandlerTest {
    @Test
    fun `fatal report is saved before chaining the same crash to Android`() {
        val calls = mutableListOf<String>()
        val thread = Thread.currentThread()
        val error = IllegalStateException("Crash")
        val previous = Thread.UncaughtExceptionHandler { receivedThread, receivedError ->
            calls += "chain"
            assertSame(thread, receivedThread)
            assertSame(error, receivedError)
        }
        val handler = ProductionCrashHandler(previous) { receivedError ->
            calls += "save"
            assertSame(error, receivedError)
        }

        handler.uncaughtException(thread, error)

        assertEquals(listOf("save", "chain"), calls)
    }

    @Test
    fun `exhausted memory while saving cannot prevent Android crash handling`() {
        val calls = mutableListOf<String>()
        val error = IllegalArgumentException("Original crash")
        val previous = Thread.UncaughtExceptionHandler { _, receivedError ->
            calls += "chain"
            assertSame(error, receivedError)
        }
        val handler = ProductionCrashHandler(previous) {
            calls += "save"
            throw OutOfMemoryError("Unable to allocate crash report")
        }

        handler.uncaughtException(Thread.currentThread(), error)

        assertEquals(listOf("save", "chain"), calls)
    }
}
