package com.kumpello.whereiseveryone.feature.main.ui.settings

import android.util.Log
import androidx.compose.ui.platform.UriHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber

class SettingsLinkTest {
    @Test
    fun `available handler opens link without showing unavailable feedback`() {
        var openedUri: String? = null
        var unavailable = false
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                openedUri = uri
            }
        }

        openSettingsLink(handler, "https://example.com/support/") { unavailable = true }

        assertEquals("https://example.com/support/", openedUri)
        assertFalse(unavailable)
    }

    @Test
    fun `unavailable handler shows feedback and reports only safe operation`() {
        val warnings = mutableListOf<String>()
        var unavailable = false
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                throw IllegalArgumentException("Can't open $uri.", IllegalStateException("private browser details"))
            }
        }
        val tree = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority >= Log.WARN) warnings += message
                assertEquals(null, t)
            }
        }
        Timber.plant(tree)
        try {
            openSettingsLink(handler, "https://example.com/private") { unavailable = true }

            assertTrue(unavailable)
            assertEquals(listOf("Unable to open settings link"), warnings)
        } finally {
            Timber.uproot(tree)
        }
    }
}
