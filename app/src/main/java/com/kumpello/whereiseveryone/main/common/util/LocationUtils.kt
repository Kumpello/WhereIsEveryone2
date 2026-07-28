package com.kumpello.whereiseveryone.main.common.util

import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.*

object LocationUtils {

    fun convertAccuracy(accuracy: Float?): AccuracyLevel {
        if (accuracy == null) return AccuracyLevel.UNKNOWN
        return when {
            accuracy > 30f -> AccuracyLevel.TRAGIC
            accuracy > 15f -> AccuracyLevel.LOW
            accuracy > 7.5f -> AccuracyLevel.MEDIUM
            accuracy > 3f -> AccuracyLevel.HIGH
            else -> AccuracyLevel.PERFECT
        }
    }

    fun calculateDistance(
        lat1: Double, lon1: Double, alt1: Double?,
        lat2: Double, lon2: Double, alt2: Double?
    ): Double {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2).pow(2.0) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val horizontalDistance = r * c
        val verticalDistance = (alt2 ?: 0.0) - (alt1 ?: 0.0)

        return sqrt(horizontalDistance.pow(2.0) + verticalDistance.pow(2.0))
    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360) % 360

        return bearing.toFloat()
    }

    fun convertAlt(userAlt: Double?, friendAlt: Double?): AltDifference {
        if (userAlt == null || friendAlt == null) return AltDifference.SOMEWHAT_SAME
        val difference = friendAlt - userAlt
        return when {
            difference < -50.0 -> AltDifference.WAY_LOWER
            difference > 50.0 -> AltDifference.WAY_HIGHER
            else -> AltDifference.SOMEWHAT_SAME
        }
    }

    fun convertLastUpdate(lastUpdate: Long, now: Long = System.currentTimeMillis()): LastUpdateAge {
        val duration = now - lastUpdate
        return when {
            duration < 60_000L -> LastUpdateAge.FRESH
            duration < 300_000L -> LastUpdateAge.NEW
            duration < 900_000L -> LastUpdateAge.SOMEWHAT_NEW
            duration < 1_800_000L -> LastUpdateAge.SOMEWHAT_OLD
            duration < 3_600_000L -> LastUpdateAge.OLD
            else -> LastUpdateAge.OLD_AS_FUCK
        }
    }

    fun formatLastUpdate(timestamp: Long): String {
        val javaInstant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
            .withZone(ZoneId.systemDefault())
        return formatter.format(javaInstant)
    }

    fun formatDate(timestamp: Long): String {
        val javaInstant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault())
        return formatter.format(javaInstant)
    }

    fun formatSpeed(speed: Float?): String? {
        if (speed == null) return null
        val kmh = speed * 3.6f
        return String.format(java.util.Locale.US, "%.1f km/h", kmh)
    }
}
