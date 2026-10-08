package com.kumpello.whereiseveryone.data.logging

import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class HttpLoggingTest {
    private data class Entry(val priority: Int, val tag: String?, val message: String)
    private val entries = mutableListOf<Entry>()
    private val tree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            entries += Entry(priority, tag, message)
        }
    }

    @Before
    fun setUp() = Timber.plant(tree)

    @After
    fun tearDown() = Timber.uproot(tree)

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
