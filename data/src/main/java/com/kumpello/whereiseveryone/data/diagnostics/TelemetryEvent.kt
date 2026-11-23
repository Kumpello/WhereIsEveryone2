package com.kumpello.whereiseveryone.data.diagnostics

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class TelemetryEvent(
    @Json(name = "event_id") val eventId: String,
    @Json(name = "occurred_at") val occurredAt: Long,
    val type: String,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "tracking_mode") val trackingMode: String,
    val component: String,
    val message: String? = null,
    @Json(name = "error_type") val errorType: String? = null,
    @Json(name = "stack_trace") val stackTrace: String? = null,
    val battery: BatteryTelemetry? = null,
)

@JsonClass(generateAdapter = true)
data class BatteryTelemetry(
    @Json(name = "duration_ms") val durationMs: Long,
    @Json(name = "start_level_percent") val startLevelPercent: Int,
    @Json(name = "end_level_percent") val endLevelPercent: Int,
    val charging: Boolean,
    @Json(name = "start_charge_uah") val startChargeUah: Long? = null,
    @Json(name = "end_charge_uah") val endChargeUah: Long? = null,
)

@JsonClass(generateAdapter = true)
internal data class TelemetryBatch(val events: List<TelemetryEvent>)

@JsonClass(generateAdapter = true)
internal data class TelemetrySession(@Json(name = "ID") val ownerId: String? = null)

@JsonClass(generateAdapter = true)
internal data class PendingCrash(val event: TelemetryEvent, val ownerId: String? = null)
