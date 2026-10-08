package com.kumpello.whereiseveryone.feature.main.sharing.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NdefApduProcessorTest {
    private var now = 0L
    private val session = NfcSharingSession { now }
    private val preparedUsernames = mutableListOf<String>()
    private val file = byteArrayOf(0, 4, 10, 20, 30, 40)
    private var completions = 0
    private val processor = NdefApduProcessor(session, {
        preparedUsernames += it
        file
    }, { completions++ })

    private val selectAid = hex("00A4040007D276000085010100")
    private val selectNdef = hex("00A4000C02E104")
    private val readLength = hex("00B0000002")
    private val readContent = hex("00B0000204")
    private val success = hex("9000")
    private val failure = hex("6A82")

    private fun beginTransfer() {
        session.start("alice")
        assertArrayEquals(success, processor.process(selectAid, true))
        assertArrayEquals(success, processor.process(selectNdef, true))
    }

    @Test
    fun `unsolicited selections and reads never prepare or serve a profile`() {
        for (command in listOf(selectAid, selectNdef, readLength, readContent)) {
            assertArrayEquals(failure, processor.process(command, true))
        }
        assertEquals(emptyList<String>(), preparedUsernames)
        assertEquals(0, completions)
    }

    @Test
    fun `closing sharing denies already cached NDEF reads`() {
        beginTransfer()
        assertArrayEquals(file.copyOfRange(0, 2) + success, processor.process(readLength, true))
        session.stop()
        assertArrayEquals(failure, processor.process(readContent, true))
        assertEquals(0, completions)
    }

    @Test
    fun `expiry is enforced mid-transfer without waiting for a UI timer`() {
        beginTransfer()
        now = NfcSharingSession.TIMEOUT_MILLIS - 1
        assertArrayEquals(file.copyOfRange(0, 2) + success, processor.process(readLength, true))
        now++
        assertArrayEquals(failure, processor.process(readContent, true))
        assertArrayEquals(failure, processor.process(selectAid, true))
        assertEquals(0, completions)
    }

    @Test
    fun `device lock revokes authorization and unlock cannot resume the cached transfer`() {
        beginTransfer()
        assertArrayEquals(failure, processor.process(readContent, false))
        assertNull(session.current())
        assertArrayEquals(failure, processor.process(readContent, true))
        assertArrayEquals(failure, processor.process(selectAid, true))
        assertEquals(0, completions)
    }

    @Test
    fun `locked device cannot select a profile even during an active session`() {
        session.start("alice")
        assertArrayEquals(failure, processor.process(selectAid, false))
        assertEquals(emptyList<String>(), preparedUsernames)
    }

    @Test
    fun `new session requires fresh selection and cannot reuse cached profile`() {
        beginTransfer()
        session.stop()
        session.start("bob")
        assertArrayEquals(failure, processor.process(readContent, true))
        assertArrayEquals(failure, processor.process(selectNdef, true))
        assertArrayEquals(success, processor.process(selectAid, true))
        assertEquals(listOf("alice", "bob"), preparedUsernames)
    }

    @Test
    fun `successful transfer consumes authorization before notifying UI`() {
        beginTransfer()
        assertArrayEquals(file.copyOfRange(0, 2) + success, processor.process(readLength, true))
        assertEquals(0, completions)
        assertArrayEquals(file.copyOfRange(2, 6) + success, processor.process(readContent, true))
        assertNull(session.current())
        assertEquals(1, completions)
        assertArrayEquals(failure, processor.process(readContent, true))
        assertArrayEquals(failure, processor.process(selectAid, true))
    }

    @Test
    fun `deactivation discards selection but permits a fresh tap within the session`() {
        beginTransfer()
        processor.reset()
        assertArrayEquals(failure, processor.process(readContent, true))
        assertArrayEquals(success, processor.process(selectAid, true))
    }

    @Test
    fun `incomplete and oversized reads fail without consuming the active transfer`() {
        beginTransfer()
        val malformedCommands = (0 until readLength.size).map(readLength::copyOf) + listOf(
            hex("00B000000200"),
            hex("00B00000000002")
        )
        for (command in malformedCommands) {
            assertArrayEquals(failure, processor.process(command, true))
        }
        assertEquals("alice", session.current()?.username)
        assertEquals(0, completions)
        assertArrayEquals(file.copyOfRange(0, 2) + success, processor.process(readLength, true))
        assertArrayEquals(file.copyOfRange(2, 6) + success, processor.process(readContent, true))
        assertEquals(1, completions)
    }

    @Test
    fun `capability reads and malformed commands cannot complete sharing`() {
        beginTransfer()
        assertArrayEquals(success, processor.process(hex("00A4000C02E103"), true))
        processor.process(hex("00B0000000"), true)
        for (command in listOf(byteArrayOf(), hex("00"), hex("00B0"), hex("00B000"))) {
            assertArrayEquals(failure, processor.process(command, true))
        }
        assertEquals(0, completions)
    }

    private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
