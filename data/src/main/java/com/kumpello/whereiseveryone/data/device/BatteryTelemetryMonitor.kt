package com.kumpello.whereiseveryone.data.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.kumpello.whereiseveryone.data.diagnostics.BatteryTelemetry
import com.kumpello.whereiseveryone.data.diagnostics.DiagnosticsReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.minutes

/** Device-wide battery change while each tracking mode is active; not app-exclusive usage. */
class BatteryTelemetryMonitor(
    context: Context,
    private val reporter: DiagnosticsReporter,
) {
    private val context = context.applicationContext
    private val started = AtomicBoolean()
    private val requestedMode = AtomicReference("inactive")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val overflowed = AtomicBoolean()
    private val commands = Channel<Command>(64, BufferOverflow.DROP_OLDEST) { overflowed.set(true) }

    private val receiver = object : BroadcastReceiver() {
        private var previousCharging: Boolean? = null

        override fun onReceive(context: Context, intent: Intent) {
            val charging = intent.isBatteryCharging()
            if (charging == previousCharging) return
            previousCharging = charging
            commands.trySend(Command.BatteryChanged(intent))
        }
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        reporter.setTrackingMode(requestedMode.get())
        scope.launch {
            try {
                val sticky = ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                val ticker = launch {
                    while (isActive) {
                        delay(15.minutes)
                        commands.send(Command.Sample)
                    }
                }
                try {
                    collectSamples(sticky)
                } finally {
                    ticker.cancel()
                    context.unregisterReceiver(receiver)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                reporter.reportError("BatteryTelemetry", "Unable to monitor battery", exception)
            } finally {
                commands.close()
            }
        }
    }

    fun setTrackingMode(mode: String) {
        if (requestedMode.getAndSet(mode) == mode) return
        reporter.setTrackingMode(mode)
        if (started.get()) commands.trySend(Command.ModeChanged(mode))
    }

    private suspend fun collectSamples(initialBattery: Intent?) {
        var mode = requestedMode.get()
        val window = BatteryTelemetryWindow()
        snapshot(initialBattery)?.let { window.update(it, mode) }

        for (command in commands) {
            if (overflowed.getAndSet(false)) {
                // A dropped transition makes the open window unreliable. Start again from current state.
                while (commands.tryReceive().isSuccess) {
                    // Discard queued commands that precede the new baseline.
                }
                window.reset()
                mode = requestedMode.get()
                snapshot(null)?.let { window.update(it, mode) }
                continue
            }

            val changedMode = (command as? Command.ModeChanged)?.mode
            if (changedMode == mode) continue
            val nextMode = changedMode ?: mode

            val sample = snapshot((command as? Command.BatteryChanged)?.intent)
            if (sample == null) {
                window.reset()
            } else {
                window.update(sample, nextMode, flush = command is Command.Sample)?.let { completed ->
                    // The boundary sample closes the interval spent in the preceding mode.
                    reporter.recordBattery(sample = completed, trackingMode = mode)
                }
            }
            mode = nextMode
        }
    }

    private fun snapshot(intent: Intent?): BatteryTelemetrySnapshot? {
        val battery = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (scale <= 0 || level !in 0..scale) return null
        val charge = context.getSystemService(BatteryManager::class.java)
            .getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            .takeIf { it >= 0 }
        return BatteryTelemetrySnapshot(
            elapsedRealtimeMs = SystemClock.elapsedRealtime(),
            levelPercent = (level.toLong() * 100 / scale).toInt(),
            charging = battery.isBatteryCharging(),
            chargeUah = charge,
        )
    }

    private sealed interface Command {
        data class ModeChanged(val mode: String) : Command
        data class BatteryChanged(val intent: Intent) : Command
        data object Sample : Command
    }
}

internal class BatteryTelemetryWindow {
    private var start: BatteryTelemetrySnapshot? = null
    private var mode: String? = null

    fun update(snapshot: BatteryTelemetrySnapshot, mode: String, flush: Boolean = false): BatteryTelemetry? {
        val previous = start
        if (previous != null && this.mode == mode && previous.charging == snapshot.charging && !flush) {
            return null
        }

        start = snapshot
        this.mode = mode
        if (previous == null || snapshot.elapsedRealtimeMs <= previous.elapsedRealtimeMs) return null

        return BatteryTelemetry(
            durationMs = snapshot.elapsedRealtimeMs - previous.elapsedRealtimeMs,
            startLevelPercent = previous.levelPercent,
            endLevelPercent = snapshot.levelPercent,
            charging = previous.charging,
            startChargeUah = previous.chargeUah,
            endChargeUah = snapshot.chargeUah,
        )
    }

    fun reset() {
        start = null
        mode = null
    }
}

internal data class BatteryTelemetrySnapshot(
    val elapsedRealtimeMs: Long,
    val levelPercent: Int,
    val charging: Boolean,
    val chargeUah: Long?,
)
