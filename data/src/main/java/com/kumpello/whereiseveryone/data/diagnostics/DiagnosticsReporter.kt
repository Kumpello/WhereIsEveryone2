package com.kumpello.whereiseveryone.data.diagnostics

import com.kumpello.whereiseveryone.data.local.database.DiagnosticsEventDao
import com.kumpello.whereiseveryone.data.local.database.DiagnosticsEventEntity
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

/** Process-owned, bounded diagnostics queue. Transport failures never log back into Timber. */
class DiagnosticsReporter internal constructor(
    private val dao: DiagnosticsEventDao,
    private val api: TelemetryApi,
    private val preferences: PreferencesManager,
    private val crashStore: CrashReportStore,
    private val appVersion: String,
    private val enabled: Boolean,
    moshi: Moshi,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val eventAdapter = moshi.adapter(TelemetryEvent::class.java)
    private val batchAdapter = moshi.adapter(TelemetryBatch::class.java)
    private val sessionAdapter = moshi.adapter(TelemetrySession::class.java)
    private val started = AtomicBoolean()
    private val storageMutex = Mutex()
    private val pending = Channel<QueuedEvent>(128, BufferOverflow.DROP_OLDEST)
    private val uploadSignal = Channel<Unit>(Channel.CONFLATED)
    @Volatile private var trackingMode = "unknown"

    fun start() {
        if (!enabled || !started.compareAndSet(false, true)) return
        scope.launch { persistQueuedEvents() }
        scope.launch {
            while (isActive) {
                try {
                    preferences.observe(PreferencesKey.AuthToken).distinctUntilChanged().collect {
                        uploadSignal.trySend(Unit)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Retry below so an observer failure does not stop upload wakeups.
                }
                // Also wait before resubscribing after normal completion.
                delay(1.minutes)
            }
        }
        scope.launch {
            var retryDelay = 1.minutes
            while (isActive) {
                // Wait for the first auth emission too, so startup uses a single drain budget.
                // Without an auth change, the timer controls batching and retries.
                withTimeoutOrNull(retryDelay) { uploadSignal.receive() }
                val success = flushAvailableBatches()
                retryDelay = if (success) 1.minutes else (retryDelay * 2).coerceAtMost(15.minutes)
            }
        }
    }

    fun setTrackingMode(mode: String) {
        trackingMode = mode.takeIf { it in MODES } ?: "unknown"
    }

    fun reportError(component: String, message: String, throwable: Throwable? = null, fatal: Boolean = false) {
        if (!enabled || (throwable is CancellationException)) return
        val event = errorEvent(appVersion, trackingMode, component, message, throwable, fatal, now())
        pending.trySend(QueuedEvent(event, currentOwner()))
    }

    fun recordBattery(sample: BatteryTelemetry, trackingMode: String) {
        if (!enabled) return
        val event = TelemetryEvent(
            eventId = UUID.randomUUID().toString(),
            occurredAt = now(),
            type = "battery",
            appVersion = appVersion.take(64),
            trackingMode = trackingMode.takeIf { it in MODES } ?: "unknown",
            component = "BatteryTelemetry",
            battery = sample,
        )
        pending.trySend(QueuedEvent(event, currentOwner()))
    }

    /** Called only by the uncaught-exception handler, before chaining to Android's handler. */
    fun saveFatal(throwable: Throwable) {
        if (enabled) crashStore.save(throwable, trackingMode, currentOwner())
    }

    /** OS-reported metadata only, used for native crashes and ANRs after the next startup. */
    suspend fun recordPreviousExit(reason: String, occurredAt: Long, eventId: String) {
        if (!enabled || (reason !in PREVIOUS_EXIT_REASONS) || (occurredAt <= 0)) return
        // Exit history can be available before the startup token observer warms the cache.
        val ownerId = ownerFromToken(preferences.get(PreferencesKey.AuthToken))
        val event = TelemetryEvent(
            eventId = eventId,
            occurredAt = occurredAt,
            type = "fatal",
            appVersion = "unknown",
            trackingMode = "unknown",
            component = "PreviousProcessExit",
            message = "Previous process terminated abnormally",
            errorType = reason,
        )
        pending.trySend(QueuedEvent(event, ownerId))
    }

    fun close() {
        pending.close()
        scope.cancel()
    }

    internal suspend fun flush(): Boolean = flushBatch() != FlushResult.RETRY

    internal fun ownerFromToken(token: String?): String? = try {
        token?.split('.')?.takeIf { it.size == 3 }?.get(1)?.let { payload ->
            sessionAdapter.fromJson(String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8))
                ?.ownerId?.takeIf { it.matches(Regex("[a-fA-F0-9]{24}")) }?.lowercase()
        }
    } catch (_: Exception) {
        null
    }

    private suspend fun persistQueuedEvents() {
        persistAndTrim {
            crashStore.read()?.let { (event, ownerId) ->
                insertEvent(event, ownerId)
                // Clear only the journal entry Room accepted. A newer crash may have replaced it.
                crashStore.clear(event.eventId)
            }
        }
        for ((event, ownerId) in pending) {
            persistAndTrim { insertEvent(event, ownerId) }
        }
    }

    private suspend fun persistAndTrim(persist: suspend () -> Unit) {
        try {
            storageMutex.withLock {
                persist()
                trimQueue()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A storage failure must not crash the application or report another diagnostic.
        }
    }

    private suspend fun insertEvent(event: TelemetryEvent, ownerId: String?) {
        dao.insert(DiagnosticsEventEntity(event.eventId, event.occurredAt, eventAdapter.toJson(event), ownerId))
    }

    private suspend fun trimQueue() {
        dao.purgeBefore(now() - RETENTION_MS)
        dao.prune(MAX_EVENTS)
    }

    private suspend fun flushAvailableBatches(): Boolean {
        repeat(MAX_BATCHES_PER_WAKE) {
            when (flushBatch()) {
                FlushResult.DONE -> return true
                FlushResult.RETRY -> return false
                FlushResult.MORE_AVAILABLE -> Unit
            }
        }
        return true
    }

    private suspend fun flushBatch(): FlushResult {
        return try {
            storageMutex.withLock { trimQueue() }
            val token = preferences.get(PreferencesKey.AuthToken) ?: return FlushResult.DONE
            val ownerId = ownerFromToken(token) ?: return FlushResult.DONE
            val queued = storageMutex.withLock { dao.oldest(ownerId, MAX_BATCH_EVENTS) }
            val (payload, eventIds) = prepareBatch(queued) ?: return FlushResult.DONE

            // A token can change while the batch is being prepared.
            if (preferences.get(PreferencesKey.AuthToken) != token) return FlushResult.DONE

            val response = api.upload(
                "Bearer $token",
                payload.toRequestBody("application/json".toMediaType()),
            )
            // Drop permanently rejected payloads so they cannot block newer events.
            if (!response.isSuccessful && (response.code() !in PERMANENT_FAILURE_CODES)) {
                return FlushResult.RETRY
            }
            storageMutex.withLock { dao.delete(eventIds) }
            if (queued.size == MAX_BATCH_EVENTS || eventIds.size < queued.size) {
                FlushResult.MORE_AVAILABLE
            } else {
                FlushResult.DONE
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            FlushResult.RETRY
        }
    }

    private suspend fun prepareBatch(queued: List<DiagnosticsEventEntity>): UploadBatch? {
        val events = mutableListOf<TelemetryEvent>()
        val eventIds = mutableListOf<String>()
        var payload: String? = null
        for ((id, _, storedPayload) in queued) {
            val event = try {
                eventAdapter.fromJson(storedPayload)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (event == null) {
                storageMutex.withLock { dao.delete(listOf(id)) }
                continue
            }
            val candidate = batchAdapter.toJson(TelemetryBatch(events + event))
            if (candidate.toByteArray(Charsets.UTF_8).size > MAX_BATCH_BYTES) {
                // A row too large on its own can never fit in a later batch.
                if (events.isEmpty()) storageMutex.withLock { dao.delete(listOf(id)) }
                break
            }
            events += event
            eventIds += id
            payload = candidate
        }
        return payload?.let { UploadBatch(it, eventIds) }
    }

    private fun currentOwner(): String? = ownerFromToken(preferences.peek(PreferencesKey.AuthToken))

    private companion object {
        const val MAX_EVENTS = 500
        const val MAX_BATCH_EVENTS = 10
        const val MAX_BATCHES_PER_WAKE = 5
        const val MAX_BATCH_BYTES = 15 * 1024
        const val RETENTION_MS = 7 * 24 * 60 * 60_000L
        val PERMANENT_FAILURE_CODES = setOf(400, 413, 422)
        val PREVIOUS_EXIT_REASONS = setOf("NativeCrash", "ApplicationNotResponding")
        val MODES = setOf("charging", "car", "foreground", "background", "inactive", "unknown")
    }

    private data class QueuedEvent(val event: TelemetryEvent, val ownerId: String?)

    private data class UploadBatch(val payload: String, val eventIds: List<String>)

    private enum class FlushResult {
        DONE,
        RETRY,
        MORE_AVAILABLE,
    }
}
