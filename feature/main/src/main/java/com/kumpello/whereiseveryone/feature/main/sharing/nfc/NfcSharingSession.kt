package com.kumpello.whereiseveryone.feature.main.sharing.nfc

import android.os.SystemClock

/** Deliberately memory-only: starting the HCE service must never authorize sharing. */
class NfcSharingSession(
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime
) {
    private var active: Session? = null

    @Synchronized
    fun start(username: String): Boolean {
        active = null
        val name = username.trim()
        if (name.isEmpty()) return false
        active = Session(name, elapsedRealtime() + TIMEOUT_MILLIS)
        return true
    }

    @Synchronized
    fun current(): Session? {
        if (active?.let { elapsedRealtime() >= it.expiresAt } == true) active = null
        return active
    }

    @Synchronized
    fun stop() {
        active = null
    }

    class Session internal constructor(val username: String, internal val expiresAt: Long)

    companion object {
        const val TIMEOUT_MILLIS = 60_000L
    }
}
