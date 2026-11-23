package com.kumpello.whereiseveryone.data.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Reason-only summaries from Android's bounded exit history; no native or ANR traces are read. */
class PreviousProcessExitReporter(
    context: Context,
    private val reporter: DiagnosticsReporter,
) {
    private val context = context.applicationContext
    private val started = AtomicBoolean()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    collectPreviousExits()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    reporter.reportError("PreviousProcessExit", "Unable to inspect previous process exits", exception)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun collectPreviousExits() {
        val marker = context.getSharedPreferences("diagnostics_previous_exits", Context.MODE_PRIVATE)
        val reportedAt = marker.getLong("last_timestamp", 0L)
        val exits = context.getSystemService(ActivityManager::class.java)
            .getHistoricalProcessExitReasons(context.packageName, 0, 16)
            .mapNotNull { exit ->
                val reason = when (exit.reason) {
                    ApplicationExitInfo.REASON_ANR -> "ApplicationNotResponding"
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> "NativeCrash"
                    else -> return@mapNotNull null
                }
                PreviousProcessExitRecord(reason, exit.timestamp, exit.pid)
            }
        val reports = selectPreviousProcessExits(exits, reportedAt, System.currentTimeMillis())
        reports.forEach { reporter.recordPreviousExit(it.reason, it.occurredAt, it.eventId) }
        reports.maxOfOrNull { it.occurredAt }?.let { timestamp ->
            // This best-effort watermark follows enqueueing. Deterministic IDs deduplicate retries
            // if Android terminates us before apply reaches disk; queue limits can still drop reports.
            marker.edit { putLong("last_timestamp", timestamp) }
        }
    }
}

internal fun selectPreviousProcessExits(
    exits: List<PreviousProcessExitRecord>,
    reportedAt: Long,
    now: Long,
): List<PreviousProcessExitReport> = exits.asSequence()
    .filter { it.reason in setOf("NativeCrash", "ApplicationNotResponding") }
    .filter { it.occurredAt > reportedAt && it.occurredAt in 1..now }
    .sortedByDescending { it.occurredAt }
    .take(16)
    .map { exit ->
        PreviousProcessExitReport(
            reason = exit.reason,
            occurredAt = exit.occurredAt,
            eventId = UUID.nameUUIDFromBytes(
                "previous-process-exit|${exit.occurredAt}|${exit.reason}|${exit.pid}".toByteArray(Charsets.UTF_8),
            ).toString(),
        )
    }
    .distinctBy { it.eventId }
    .sortedBy { it.occurredAt }
    .toList()

internal data class PreviousProcessExitRecord(val reason: String, val occurredAt: Long, val pid: Int)

internal data class PreviousProcessExitReport(val reason: String, val occurredAt: Long, val eventId: String)
