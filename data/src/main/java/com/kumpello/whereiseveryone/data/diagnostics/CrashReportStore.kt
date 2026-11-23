package com.kumpello.whereiseveryone.data.diagnostics

import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/** A small crash-only journal: synchronous persistence is necessary before Android terminates us. */
class CrashReportStore(private val file: File, private val appVersion: String) {
    private val adapter = Moshi.Builder().build().adapter(PendingCrash::class.java)

    @Synchronized
    fun save(throwable: Throwable, trackingMode: String = "unknown", ownerId: String? = null) {
        if (throwable is CancellationException) return
        val crash = PendingCrash(
            errorEvent(appVersion, trackingMode, "UncaughtException", "Uncaught application exception", throwable, true),
            ownerId,
        )
        val temporary = File(file.parentFile, "${file.name}.tmp")
        file.parentFile?.mkdirs()
        FileOutputStream(temporary).use { output ->
            output.write(adapter.toJson(crash).toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    @Synchronized
    internal fun read(): PendingCrash? {
        if (!file.exists()) return null
        if (file.length() > 16_384) {
            file.delete()
            return null
        }
        return try {
            adapter.fromJson(file.readText())
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    @Synchronized
    internal fun clear(expectedEventId: String) {
        if (read()?.event?.eventId == expectedEventId) file.delete()
    }
}

internal fun errorEvent(
    appVersion: String,
    trackingMode: String,
    component: String,
    message: String,
    throwable: Throwable?,
    fatal: Boolean,
    now: Long = System.currentTimeMillis(),
): TelemetryEvent = TelemetryEvent(
    eventId = UUID.randomUUID().toString(),
    occurredAt = now,
    type = if (fatal) "fatal" else "error",
    appVersion = appVersion.take(64),
    trackingMode = trackingMode,
    component = component.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(64).ifBlank { "Application" },
    message = safeDiagnosticMessage(message),
    errorType = throwable?.javaClass?.name?.filter { it in ' '..'~' }?.take(128),
    // Frame names contain code identifiers only; never include throwable messages or causes.
    stackTrace = throwable?.stackTrace?.asSequence()
        ?.toList()
        ?.let { frames ->
            frames.filter { it.className.startsWith("com.kumpello.whereiseveryone.") }
                .ifEmpty { frames } // Preserve retraceable names when R8 obfuscates the app package.
        }
        ?.take(16)?.joinToString("\n")?.filter { it == '\n' || it in ' '..'~' }?.take(2048)?.ifBlank { null },
)

internal fun safeDiagnosticMessage(message: String): String = message.lineSequence().firstOrNull().orEmpty()
    .replace(Regex("https?://\\S+"), "[url]")
    .replace(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[email]")
    .replace(Regex("(?i)(bearer\\s+|[ps]k\\.)\\S+"), "[credential]")
    .replace(Regex("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"), "[credential]")
    .replace(Regex("-?\\d{1,3}\\.\\d{3,}"), "[number]")
    .filter { it in ' '..'~' }
    .take(512)
