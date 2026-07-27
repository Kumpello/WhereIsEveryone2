package com.kumpello.whereiseveryone.common.extension

import java.util.Locale

fun formatDistance(distanceInMeters: Double): String {
    return if (distanceInMeters >= 1000) {
        String.format(Locale.getDefault(), "%.2f km", distanceInMeters / 1000)
    } else {
        String.format(Locale.getDefault(), "%d m", distanceInMeters.toInt())
    }
}

fun lerp(start: Double, end: Double, fraction: Double): Double = start + (end - start) * fraction

fun lerpBearing(start: Double, end: Double, fraction: Double): Double {
    var diff = (end - start) % 360
    if (diff > 180) diff -= 360
    if (diff < -180) diff += 360
    return start + diff * fraction
}
