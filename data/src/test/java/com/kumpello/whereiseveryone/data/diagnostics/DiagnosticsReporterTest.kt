package com.kumpello.whereiseveryone.data.diagnostics

import com.kumpello.whereiseveryone.data.local.database.DiagnosticsEventDao
import com.kumpello.whereiseveryone.data.local.database.DiagnosticsEventEntity
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.IOException
import java.util.Base64
import java.util.UUID

class DiagnosticsReporterTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val dao = mockk<DiagnosticsEventDao>(relaxed = true)
    private val api = mockk<TelemetryApi>()
    private val preferences = mockk<PreferencesManager>()
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(TelemetryEvent::class.java)
    private val owner = "0123456789abcdef01234567"
    private val token = token(owner)

    @Test
    fun `successful delivery removes only acknowledged event IDs`() = runTest {
        val row = row()
        val reporter = reporter()
        coEvery { preferences.get(PreferencesKey.AuthToken) } returns token
        coEvery { dao.oldest(owner, 10) } returns listOf(row)
        var body = ""
        coEvery { api.upload("Bearer $token", any()) } answers {
            body = secondArg<RequestBody>().let { request -> Buffer().also(request::writeTo).readUtf8() }
            Response.success(Unit)
        }

        assertTrue(reporter.flush())

        coVerify(exactly = 1) { dao.delete(listOf(row.id)) }
        assertEquals(row.id, moshi.adapter(TelemetryBatch::class.java).fromJson(body)!!.events.single().eventId)
        reporter.close()
    }

    @Test
    fun `network and server failures retain events for retry with same IDs`() = runTest {
        val row = row()
        val reporter = reporter()
        coEvery { preferences.get(PreferencesKey.AuthToken) } returns token
        coEvery { dao.oldest(owner, 10) } returns listOf(row)
        coEvery { api.upload(any(), any()) } throws IOException("offline")

        assertFalse(reporter.flush())
        coEvery { api.upload(any(), any()) } returns Response.error(503, "".toResponseBody("text/plain".toMediaType()))
        assertFalse(reporter.flush())

        coVerify(exactly = 0) { dao.delete(any()) }
        reporter.close()
    }

    @Test
    fun `session change during preparation does not upload with another accounts credentials`() = runTest {
        val reporter = reporter()
        coEvery { preferences.get(PreferencesKey.AuthToken) } returnsMany listOf(token, token("aaaaaaaaaaaaaaaaaaaaaaaa"))
        coEvery { dao.oldest(owner, 10) } returns listOf(row())

        assertTrue(reporter.flush())

        coVerify(exactly = 0) { api.upload(any(), any()) }
        coVerify(exactly = 0) { dao.delete(any()) }
        reporter.close()
    }

    @Test
    fun `batch stays below request byte limit even with maximum stack traces`() = runTest {
        val rows = List(10) { row("x".repeat(2048)) }
        val reporter = reporter()
        coEvery { preferences.get(PreferencesKey.AuthToken) } returns token
        coEvery { dao.oldest(owner, 10) } returns rows
        var bytes = 0L
        coEvery { api.upload(any(), any()) } answers {
            bytes = secondArg<RequestBody>().contentLength()
            Response.success(Unit)
        }

        assertTrue(reporter.flush())

        assertTrue(bytes in 1..15 * 1024)
        coVerify { dao.delete(match { it.isNotEmpty() && it.size < rows.size }) }
        reporter.close()
    }

    @Test
    fun `anonymous sessions wait and coroutine cancellation propagates`() = runTest {
        val reporter = reporter()
        coEvery { preferences.get(PreferencesKey.AuthToken) } returns null
        assertTrue(reporter.flush())
        coVerify(exactly = 0) { api.upload(any(), any()) }
        coEvery { preferences.get(PreferencesKey.AuthToken) } throws CancellationException()
        var cancelled = false
        try {
            reporter.flush()
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        reporter.close()
    }

    @Test
    fun `ownership uses stable account ID not username or per-token ID`() {
        val reporter = reporter()
        assertEquals(owner, reporter.ownerFromToken(token))
        assertEquals(null, reporter.ownerFromToken("malformed"))
        assertEquals(null, reporter.ownerFromToken(token("not-an-account-id")))
        reporter.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `initial token wake drains at most five successful batches without an immediate second drain`() = runTest {
        val rows = List(10) { row() }
        val reporter = DiagnosticsReporter(
            dao, api, preferences, CrashReportStore(temporaryFolder.newFile(), "0.9"),
            "0.9", true, moshi, scope = backgroundScope,
        )
        every { preferences.observe(PreferencesKey.AuthToken) } returns flowOf(token)
        coEvery { preferences.get(PreferencesKey.AuthToken) } returns token
        coEvery { dao.oldest(owner, 10) } returns rows
        coEvery { api.upload(any(), any()) } returns Response.success(Unit)

        reporter.start()
        runCurrent()

        coVerify(exactly = 5) { api.upload("Bearer $token", any()) }
        coVerify(exactly = 5) { dao.delete(rows.map { it.id }) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `initial token wake with an empty queue does not upload`() = runTest {
        val reporter = DiagnosticsReporter(
            dao, api, preferences, CrashReportStore(temporaryFolder.newFile(), "0.9"),
            "0.9", true, moshi, scope = backgroundScope,
        )
        every { preferences.observe(PreferencesKey.AuthToken) } returns flowOf(token)
        coEvery { preferences.get(PreferencesKey.AuthToken) } returns token
        coEvery { dao.oldest(owner, 10) } returns emptyList()

        reporter.start()
        runCurrent()

        coVerify(exactly = 1) { dao.oldest(owner, 10) }
        coVerify(exactly = 0) { api.upload(any(), any()) }
    }

    @Test
    fun `switching accounts preserves separate queues and never assigns unknown reports`() = runTest {
        val queue = QueueDao()
        val first = row()
        val secondOwner = "aaaaaaaaaaaaaaaaaaaaaaaa"
        val second = row().copy(ownerId = secondOwner)
        val unknown = row().copy(ownerId = null)
        queue.insert(first)
        queue.insert(unknown)
        var currentToken = token
        val capturedToken = CompletableDeferred<Unit>()
        val resumeToken = CompletableDeferred<Unit>()
        var firstRead = true
        coEvery { preferences.get(PreferencesKey.AuthToken) } coAnswers {
            val snapshot = currentToken
            if (firstRead) {
                firstRead = false
                capturedToken.complete(Unit)
                resumeToken.await()
            }
            snapshot
        }
        val reporter = DiagnosticsReporter(
            queue, api, preferences, CrashReportStore(temporaryFolder.newFile(), "0.9"),
            "0.9", true, moshi, scope = backgroundScope, now = { first.createdAt },
        )
        val staleFlush = async { reporter.flush() }
        capturedToken.await()
        currentToken = token(secondOwner)
        queue.insert(second)
        resumeToken.complete(Unit)

        assertTrue(staleFlush.await())
        coVerify(exactly = 0) { api.upload(any(), any()) }
        assertEquals(setOf(first.id, second.id, unknown.id), queue.events.keys)

        val uploaded = mutableListOf<Pair<String, List<String>>>()
        coEvery { api.upload(any(), any()) } answers {
            val json = secondArg<RequestBody>().let { request -> Buffer().also(request::writeTo).readUtf8() }
            uploaded += firstArg<String>() to moshi.adapter(TelemetryBatch::class.java)
                .fromJson(json)!!.events.map { it.eventId }
            Response.success(Unit)
        }
        assertTrue(reporter.flush())
        assertEquals(setOf(first.id, unknown.id), queue.events.keys)
        currentToken = token
        assertTrue(reporter.flush())
        assertEquals(setOf(unknown.id), queue.events.keys)
        assertEquals(listOf(
            "Bearer ${token(secondOwner)}" to listOf(second.id),
            "Bearer $token" to listOf(first.id),
        ), uploaded)
        assertEquals(null, queue.events[unknown.id]!!.ownerId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recovering a crash preserves a newer journal saved while insertion is suspended`() = runTest {
        val store = CrashReportStore(temporaryFolder.newFile(), "0.9")
        store.save(IOException("first crash"), ownerId = owner)
        val firstId = store.read()!!.event.eventId
        val enteredInsert = CompletableDeferred<Unit>()
        val finishInsert = CompletableDeferred<Unit>()
        coEvery { dao.insert(any()) } coAnswers {
            enteredInsert.complete(Unit)
            finishInsert.await()
        }
        every { preferences.observe(PreferencesKey.AuthToken) } returns emptyFlow()
        val reporter = DiagnosticsReporter(
            dao, api, preferences, store, "0.9", true, moshi, scope = backgroundScope,
        )
        reporter.start()
        enteredInsert.await()
        store.save(IOException("new crash"), ownerId = owner)
        val newId = store.read()!!.event.eventId
        finishInsert.complete(Unit)
        runCurrent()

        coVerify { dao.insert(match { it.id == firstId }) }
        assertEquals(newId, store.read()!!.event.eventId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `signed out crash recovery enforces expiry and queue capacity`() = runTest {
        val now = System.currentTimeMillis()
        val queue = QueueDao()
        repeat(500) { queue.insert(row(occurredAt = now - 1_000 - it, ownerId = null)) }
        val expired = row(occurredAt = now - 8 * 24 * 60 * 60_000L, ownerId = null)
        queue.insert(expired)
        val store = CrashReportStore(temporaryFolder.newFile(), "0.9")
        store.save(IOException("crash"))
        val crashId = store.read()!!.event.eventId
        every { preferences.observe(PreferencesKey.AuthToken) } returns emptyFlow()
        val reporter = DiagnosticsReporter(
            queue, api, preferences, store, "0.9", true, moshi, scope = backgroundScope, now = { now },
        )

        reporter.start()
        runCurrent()

        assertEquals(500, queue.events.size)
        assertFalse(queue.events.containsKey(expired.id))
        assertTrue(queue.events.containsKey(crashId))
        assertEquals(null, store.read())
        coVerify(exactly = 0) { api.upload(any(), any()) }
    }

    @Test
    fun `signed out flush expires old reports without assigning unowned reports`() = runTest {
        val now = System.currentTimeMillis()
        val queue = QueueDao()
        val expired = row(occurredAt = now - 8 * 24 * 60 * 60_000L, ownerId = null)
        val recent = row(occurredAt = now, ownerId = null)
        queue.insert(expired)
        queue.insert(recent)
        coEvery { preferences.get(PreferencesKey.AuthToken) } returns null
        val reporter = DiagnosticsReporter(
            queue, api, preferences, CrashReportStore(temporaryFolder.newFile(), "0.9"),
            "0.9", true, moshi, scope = backgroundScope, now = { now },
        )

        assertTrue(reporter.flush())

        assertEquals(setOf(recent.id), queue.events.keys)
        assertEquals(null, queue.events[recent.id]!!.ownerId)
        coVerify(exactly = 0) { api.upload(any(), any()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `previous exits use persisted ownership before the auth cache is warm`() = runTest {
        val queue = QueueDao()
        val now = 1_700_000_000_000L
        every { preferences.peek(PreferencesKey.AuthToken) } returns null
        every { preferences.observe(PreferencesKey.AuthToken) } returns emptyFlow()
        coEvery { preferences.get(PreferencesKey.AuthToken) } returns token
        val reporter = DiagnosticsReporter(
            queue, api, preferences, CrashReportStore(temporaryFolder.newFile(), "0.9"),
            "0.9", true, moshi, scope = backgroundScope, now = { now },
        )
        val reasons = listOf("NativeCrash", "ApplicationNotResponding")

        reasons.forEach { reason ->
            reporter.recordPreviousExit(reason, now - 1_000, UUID.randomUUID().toString())
        }
        reporter.start()
        runCurrent()

        assertEquals(2, queue.events.size)
        assertEquals(setOf(owner), queue.events.values.map { it.ownerId }.toSet())
        val events = queue.events.values.map { adapter.fromJson(it.payload)!! }
        assertEquals(reasons.toSet(), events.map { it.errorType }.toSet())
        events.forEach { event ->
            assertEquals("fatal", event.type)
            assertEquals(now - 1_000, event.occurredAt)
            assertEquals("unknown", event.appVersion)
            assertEquals("unknown", event.trackingMode)
            assertEquals(null, event.stackTrace)
        }
        reporter.close()
    }

    private fun reporter() = DiagnosticsReporter(
        dao, api, preferences, CrashReportStore(temporaryFolder.newFile(), "0.9"),
        "0.9", true, moshi,
    )

    private fun row(
        stack: String? = null,
        occurredAt: Long = 1_700_000_000_000,
        ownerId: String? = owner,
    ): DiagnosticsEventEntity {
        val event = TelemetryEvent(UUID.randomUUID().toString(), occurredAt, "error", "0.9",
            "background", "HTTP", "Request failed", "IOException", stack)
        return DiagnosticsEventEntity(event.eventId, event.occurredAt, adapter.toJson(event), ownerId)
    }

    private fun token(owner: String): String = "header." +
        Base64.getUrlEncoder().withoutPadding().encodeToString("{\"ID\":\"$owner\"}".toByteArray()) + ".signature"

    private class QueueDao : DiagnosticsEventDao {
        val events = linkedMapOf<String, DiagnosticsEventEntity>()

        override suspend fun insert(event: DiagnosticsEventEntity) {
            events.putIfAbsent(event.id, event)
        }

        override suspend fun oldest(ownerId: String, limit: Int) = events.values
            .filter { it.ownerId == ownerId }
            .sortedWith(compareBy({ it.createdAt }, { it.id })).take(limit)

        override suspend fun delete(ids: List<String>) {
            ids.forEach { events.remove(it) }
        }

        override suspend fun prune(maxCount: Int) {
            val keep = events.values.sortedWith(compareByDescending<DiagnosticsEventEntity> { it.createdAt }
                .thenByDescending { it.id }).take(maxCount).map { it.id }.toSet()
            events.keys.retainAll(keep)
        }

        override suspend fun purgeBefore(cutoff: Long) {
            events.values.removeAll { it.createdAt < cutoff }
        }
    }
}
