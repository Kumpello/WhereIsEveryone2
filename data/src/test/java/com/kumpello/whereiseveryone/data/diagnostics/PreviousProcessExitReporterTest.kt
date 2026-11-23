package com.kumpello.whereiseveryone.data.diagnostics

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviousProcessExitReporterTest {
    @Test
    fun `only new ANR and native exits with valid timestamps are selected`() {
        val exits = listOf(
            PreviousProcessExitRecord("ApplicationNotResponding", 200, 1),
            PreviousProcessExitRecord("NativeCrash", 300, 2),
            PreviousProcessExitRecord("JvmCrash", 400, 3),
            PreviousProcessExitRecord("UserStopped", 500, 4),
            PreviousProcessExitRecord("LowMemory", 600, 5),
            PreviousProcessExitRecord("NativeCrash", 100, 6),
            PreviousProcessExitRecord("NativeCrash", 0, 7),
            PreviousProcessExitRecord("NativeCrash", 1_001, 8),
        )

        val reports = selectPreviousProcessExits(exits, reportedAt = 100, now = 1_000)

        assertEquals(listOf("ApplicationNotResponding", "NativeCrash"), reports.map { it.reason })
        assertEquals(listOf(200L, 300L), reports.map { it.occurredAt })
    }

    @Test
    fun `history is bounded to the newest sixteen exits and queued oldest first`() {
        val exits = (1L..100L).map { PreviousProcessExitRecord("NativeCrash", it, it.toInt()) }

        val reports = selectPreviousProcessExits(exits, reportedAt = 0, now = 100)

        assertEquals(16, reports.size)
        assertEquals((85L..100L).toList(), reports.map { it.occurredAt })
    }

    @Test
    fun `repeated history has stable deduplicated IDs without sending process IDs`() {
        val native = PreviousProcessExitRecord("NativeCrash", 500, 123)
        val exits = listOf(native, native, native.copy(pid = 124))
        val reports = selectPreviousProcessExits(exits, reportedAt = 0, now = 1_000)

        assertEquals(2, reports.size)
        assertEquals(reports, selectPreviousProcessExits(exits, reportedAt = 0, now = 1_000))
        assertNotEquals(reports[0].eventId, reports[1].eventId)
        reports.forEach { assertEquals(it.eventId, UUID.fromString(it.eventId).toString()) }
    }

    @Test
    fun `watermark prevents repeated reports on later startup`() {
        val exits = listOf(PreviousProcessExitRecord("ApplicationNotResponding", 500, 123))
        val first = selectPreviousProcessExits(exits, reportedAt = 0, now = 1_000)

        assertTrue(selectPreviousProcessExits(exits, reportedAt = first.single().occurredAt, now = 1_500).isEmpty())
    }
}
