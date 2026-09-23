package com.kumpello.whereiseveryone.common.logging

import android.util.Log
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/** Release Logcat output. Callers must use safe operation names and non-personal metadata. */
internal class ProductionTree : Timber.Tree() {
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
        Log.println(priority, tag ?: "WhereIsEveryone", safeMessage)
    }
}
