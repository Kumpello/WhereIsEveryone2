package com.kumpello.whereiseveryone.feature.main.ui.map

import androidx.compose.runtime.Immutable
import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.repository.RoadMatch
import com.kumpello.whereiseveryone.data.repository.RoadInterceptionEstimate
import com.kumpello.whereiseveryone.feature.main.ui.mapper.LocationUtils
import kotlin.math.cos

internal const val CAR_SUGGESTION_SPEED_MPS = 30.0 / 3.6
internal const val CAR_LOCATION_MAX_AGE_MS = 30_000L

internal fun LocationData.isFresh(now: Long, maxAge: Long = CAR_LOCATION_MAX_AGE_MS) =
    now - lastUpdate in 0..maxAge

internal fun LocationData.speedKmh(): Double? = speed?.toDouble()
    ?.takeIf { it.isFinite() && it >= 0 }?.times(3.6)

internal fun shouldSuggestCarMode(location: LocationData?, now: Long): Boolean =
    location != null && location.isFresh(now, 10_000L) &&
        location.speed?.let { it.isFinite() && it > CAR_SUGGESTION_SPEED_MPS } == true

/** Snap only within GPS uncertainty and while the matched road is recent. */
internal fun roadDisplayLocation(location: LocationData, match: RoadMatch?, now: Long): LocationData {
    if (match == null || !location.isCarMode || !location.isFresh(now) ||
        location.lastUpdate - match.fix.lastUpdate !in 0..10_000L) return location

    val metersPerLatitude = 111_320.0
    val metersPerLongitude = metersPerLatitude * cos(Math.toRadians(location.lat)).coerceAtLeast(0.0001)
    fun longitudeDelta(lon: Double) = (lon - location.lon + 540.0) % 360.0 - 180.0

    var closestLat = match.point.lat
    var closestLon = match.point.lon
    var closestDistance = LocationUtils.calculateDistance(location.lat, location.lon, null, closestLat, closestLon, null)
    for (index in 1 until match.road.size) {
        val from = match.road[index - 1]
        val to = match.road[index]
        val x = longitudeDelta(from.lon) * metersPerLongitude
        val y = (from.lat - location.lat) * metersPerLatitude
        val dx = longitudeDelta(to.lon) * metersPerLongitude - x
        val dy = (to.lat - from.lat) * metersPerLatitude
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared > 0) {
            val fraction = (-(x * dx + y * dy) / lengthSquared).coerceIn(0.0, 1.0)
            val px = x + fraction * dx
            val py = y + fraction * dy
            val distance = kotlin.math.hypot(px, py)
            if (distance < closestDistance) {
                closestDistance = distance
                closestLat = location.lat + py / metersPerLatitude
                closestLon = (location.lon + px / metersPerLongitude + 540.0) % 360.0 - 180.0
            }
        }
    }
    val radius = (location.accuracy?.toDouble() ?: 25.0).coerceIn(10.0, 50.0)
    return if (closestDistance <= radius) location.copy(lat = closestLat, lon = closestLon) else location
}

@Immutable
data class CarTrackingInfo(
    val friendSpeedKmh: Double?,
    val speedDifferenceKmh: Double?,
    val travelSeconds: Double?,
    val routeDistanceMeters: Double?,
    val isLocationStale: Boolean,
    val isPredictive: Boolean = false,
    val interceptionEstimate: RoadInterceptionEstimate? = null,
)

data class CarInterceptionPrediction(
    val estimate: RoadInterceptionEstimate,
    val user: LocationData,
    val driver: LocationData,
    val calculatedAt: Long,
) {
    internal fun currentEstimate(currentUser: LocationData, currentDriver: LocationData, now: Long): RoadInterceptionEstimate? {
        val age = now - calculatedAt
        if (age !in 0..30_000L || !currentUser.isCarMode || !currentDriver.isCarMode ||
            !user.sameMotionAs(currentUser) || !driver.sameMotionAs(currentDriver)) return null
        val elapsedSeconds = age / 1_000.0
        return when (estimate) {
            is RoadInterceptionEstimate.Intercept -> if ((estimate.seconds > 0 && elapsedSeconds > estimate.seconds) ||
                (estimate.seconds == 0.0 && LocationUtils.calculateDistance(currentUser.lat, currentUser.lon, null, currentDriver.lat, currentDriver.lon, null) > 20.0)) null
                else estimate.copy(seconds = (estimate.seconds - elapsedSeconds).coerceAtLeast(0.0))
            is RoadInterceptionEstimate.ArriveLater -> if (elapsedSeconds > estimate.arrivalSeconds) null
                else estimate.copy(arrivalSeconds = estimate.arrivalSeconds - elapsedSeconds)
            else -> estimate
        }
    }

    private fun LocationData.sameMotionAs(current: LocationData): Boolean {
        val before = speed?.takeIf { it.isFinite() && it >= 0 } ?: return false
        val after = current.speed?.takeIf { it.isFinite() && it >= 0 } ?: return false
        if (kotlin.math.abs(before - after) > 1f || (before == 0f) != (after == 0f)) return false
        if (before < 1f && after < 1f) return true
        val oldBearing = bearing?.takeIf { it.isFinite() }
        val newBearing = current.bearing?.takeIf { it.isFinite() }
        return if (oldBearing == null || newBearing == null) oldBearing == newBearing
        else kotlin.math.abs((oldBearing - newBearing + 540f) % 360f - 180f) <= 30f
    }
}
