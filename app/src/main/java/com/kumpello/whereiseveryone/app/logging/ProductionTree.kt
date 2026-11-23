package com.kumpello.whereiseveryone.app.logging

import android.util.Log
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/** Release diagnostics. Callers must use safe operation names and non-personal metadata. */
internal class ProductionTree : Timber.Tree() {
    private val lock = Any()
    private val startupEntries = ArrayDeque<Entry>()
    private var reporter: ((String, String, Throwable?) -> Unit)? = null

    fun attachReporter(reporter: (String, String, Throwable?) -> Unit) {
        val pending = synchronized(lock) {
            this.reporter = reporter
            startupEntries.toList().also { startupEntries.clear() }
        }
        pending.forEach { deliver(reporter, it) }
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t is CancellationException) return

        val safeMessage = if (t == null) {
            message
        } else {
            // Timber has already appended the full throwable, including messages and causes.
            // Remove that suffix; fail closed if its format ever changes.
            val trace = t.stackTraceToString()
            val context = if (message.endsWith(trace)) message.removeSuffix(trace).trimEnd() else ""
            listOf(context, "[${t.javaClass.simpleName}]").filter { it.isNotEmpty() }.joinToString(" ")
        }
        val entry = Entry(tag ?: "WhereIsEveryone", safeMessage, t)
        Log.println(priority, entry.component, entry.message)
        val target = synchronized(lock) {
            reporter.also { target ->
                if (target == null) {
                    if (startupEntries.size == MAX_STARTUP_ENTRIES) startupEntries.removeFirst()
                    startupEntries.addLast(entry)
                }
            }
        }
        if (target != null) deliver(target, entry)
    }

    private fun deliver(target: (String, String, Throwable?) -> Unit, entry: Entry) {
        try {
            target(entry.component, entry.message, entry.throwable)
        } catch (_: Exception) {
            // Diagnostics must not break the operation being logged or recursively log its own failures.
        }
    }

    private data class Entry(val component: String, val message: String, val throwable: Throwable?)

    private companion object {
        const val MAX_STARTUP_ENTRIES = 100
    }
}
