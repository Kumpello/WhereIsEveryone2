package com.kumpello.whereiseveryone.common.extension

import java.util.Locale

fun formatDistance(distanceInMeters: Double): String {
    return if (distanceInMeters >= 1000) {
        String.format(Locale.getDefault(), "%.2f km", distanceInMeters / 1000)
    } else {
        String.format(Locale.getDefault(), "%d m", distanceInMeters.toInt())
    }
}
