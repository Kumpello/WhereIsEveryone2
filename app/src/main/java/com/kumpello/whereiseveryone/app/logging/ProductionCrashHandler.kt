package com.kumpello.whereiseveryone.app.logging

/** Save locally before Android's existing crash handler terminates the process. */
internal class ProductionCrashHandler(
    private val previous: Thread.UncaughtExceptionHandler,
    private val save: (Throwable) -> Unit,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, error: Throwable) {
        try {
            save(error)
        } catch (_: Throwable) {
            // Storage failure, including exhausted memory, must not prevent the original crash handler.
        } finally {
            previous.uncaughtException(thread, error)
        }
    }
}
