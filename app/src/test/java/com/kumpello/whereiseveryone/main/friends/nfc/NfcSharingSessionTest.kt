package com.kumpello.whereiseveryone.main.friends.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcSharingSessionTest {
    private var now = 1_000L
    private val session = NfcSharingSession { now }

    @Test
    fun `new process and stopped session cannot share`() {
        assertNull(session.current())
        assertTrue(session.start("alice"))
        session.stop()
        assertNull(session.current())
        assertNull(NfcSharingSession { now }.current())
    }

    @Test
    fun `session expires at the monotonic deadline and reads cannot extend it`() {
        assertTrue(session.start(" alice "))
        now += NfcSharingSession.TIMEOUT_MILLIS - 1
        repeat(10) { assertEquals("alice", session.current()?.username) }
        now++
        assertNull(session.current())
        assertTrue(session.start("bob"))
        assertEquals("bob", session.current()?.username)
    }

    @Test
    fun `blank username never authorizes sharing or retains an old session`() {
        session.start("alice")
        assertFalse(session.start("  "))
        assertNull(session.current())
    }
}
