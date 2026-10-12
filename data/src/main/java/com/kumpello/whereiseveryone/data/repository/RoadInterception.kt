package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.network.api.DrivingRoute
import com.kumpello.whereiseveryone.data.network.api.DrivingStep
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

internal const val INTERCEPTION_HORIZON_SECONDS = 900.0
internal const val INTERCEPTION_MAX_ROAD_METERS = 30_000.0
internal const val MEETING_RADIUS_METERS = 20.0
private const val EARTH_RADIUS_METERS = 6_371_000.0

internal fun RoadPoint.distanceTo(other: RoadPoint): Double {
    val lat1 = Math.toRadians(lat)
    val lat2 = Math.toRadians(other.lat)
    val halfLat = sin((lat2 - lat1) / 2)
    val halfLon = sin(Math.toRadians(longitudeDelta(other.lon, lon)) / 2)
    val square = (halfLat * halfLat + cos(lat1) * cos(lat2) * halfLon * halfLon).coerceIn(0.0, 1.0)
    return 2 * EARTH_RADIUS_METERS * atan2(sqrt(square), sqrt(1 - square))
}

internal fun RoadPoint.ahead(bearing: Double, meters: Double): RoadPoint {
    val angle = meters / EARTH_RADIUS_METERS
    val heading = Math.toRadians(bearing)
    val latitude = Math.toRadians(lat)
    val nextLat = asin((sin(latitude) * cos(angle) + cos(latitude) * sin(angle) * cos(heading)).coerceIn(-1.0, 1.0))
    val nextLon = Math.toRadians(lon) + atan2(sin(heading) * sin(angle) * cos(latitude), cos(angle) - sin(latitude) * sin(nextLat))
    return RoadPoint(Math.toDegrees(nextLat), normalizeLongitude(Math.toDegrees(nextLon)))
}

internal fun bearingDifference(first: Double, second: Double): Double =
    kotlin.math.abs((first - second + 540.0) % 360.0 - 180.0)

/** Use only the initial road continuation; do not invent a destination or an exit choice. */
internal fun DrivingRoute.currentRoad(fix: LocationData, bearing: Double): ForwardRoadPath? {
    val steps = legs.singleOrNull()?.steps?.takeIf { it.isNotEmpty() } ?: return null
    val departure = steps.first()
    if (departure.maneuver.type != "depart" || !departure.maneuver.bearing_after.isFinite() ||
        bearingDifference(departure.maneuver.bearing_after, bearing) > 60.0) return null
    val points = mutableListOf<RoadPoint>()
    for ((index, step) in steps.withIndex()) {
        if (step.mode != "driving" || step.maneuver.type == "arrive") break
        if (index > 0 && !step.continues(steps[index - 1])) break
        if (step.geometry.coordinates.isEmpty()) return null
        for ((pointIndex, coordinate) in step.geometry.coordinates.withIndex()) {
            if (coordinate.size < 2 || !coordinate[0].isFinite() || !coordinate[1].isFinite() ||
                coordinate[0] !in -180.0..180.0 || coordinate[1] !in -90.0..90.0) return null
            val point = RoadPoint(coordinate[1], coordinate[0])
            if (pointIndex == 0 && points.lastOrNull()?.distanceTo(point)?.let { it > 10.0 } == true) return null
            if (point != points.lastOrNull()) points += point
        }
    }
    if (points.size < 2) return null
    val uncertainty = (fix.accuracy?.toDouble()?.takeIf { it.isFinite() } ?: 25.0).coerceIn(10.0, 50.0)
    if (RoadPoint(fix.lat, fix.lon).distanceTo(points.first()) > uncertainty + 10.0) return null
    return ForwardRoadPath(points).takeIf { it.lengthMeters > 0 }
}

internal fun interceptionWindow(samples: List<RoadArrivalSample>, speed: Double): InterceptionWindow? {
    fun gap(sample: RoadArrivalSample): Double? = sample.distanceMeters?.let {
        it - speed * sample.seconds - if (speed == 0.0) MEETING_RADIUS_METERS else 0.0
    }
    for ((index, sample) in samples.withIndex()) {
        val afterGap = gap(sample) ?: continue
        if (afterGap > 0.01) continue
        val previous = samples.getOrNull(index - 1)
        val beforeGap = previous?.let(::gap)
        // Never interpolate across an unreachable road pair or a routing discontinuity.
        if (previous == null || beforeGap == null || beforeGap <= 0.01) {
            return InterceptionWindow(sample, sample, sample.seconds)
        }
        val fraction = (beforeGap / (beforeGap - afterGap)).coerceIn(0.0, 1.0)
        return InterceptionWindow(previous, sample, previous.seconds + (sample.seconds - previous.seconds) * fraction)
    }
    return null
}

private fun longitudeDelta(to: Double, from: Double) = (to - from + 540.0) % 360.0 - 180.0
private fun normalizeLongitude(lon: Double) = (lon + 540.0) % 360.0 - 180.0

private fun DrivingStep.continues(previous: DrivingStep): Boolean {
    if (maneuver.modifier == "uturn") return false
    return when (maneuver.type) {
        "continue", "new name", "notification" -> true
        "merge", "fork" -> {
            val sameName = name.isNotBlank() && name.equals(previous.name, ignoreCase = true)
            val previousRefs = previous.ref.split(';').map(String::trim)
            val sameRef = ref.isNotBlank() && ref.split(';').any { it.trim() in previousRefs }
            (sameName || sameRef) && maneuver.modifier in setOf("straight", "slight left", "slight right")
        }
        else -> false
    }
}

/** Distances follow every bend in the returned road geometry, never a heading ray. */
internal class ForwardRoadPath(private val points: List<RoadPoint>) {
    private val distances = DoubleArray(points.size).apply {
        for (index in 1 until size) this[index] = this[index - 1] + points[index - 1].distanceTo(points[index])
    }
    val lengthMeters: Double get() = distances.last()

    fun pointAt(meters: Double): RoadPoint {
        require(meters in 0.0..lengthMeters)
        val found = distances.binarySearch(meters)
        if (found >= 0) return points[found]
        val next = (-found - 1).coerceIn(1, points.lastIndex)
        val fraction = (meters - distances[next - 1]) / (distances[next] - distances[next - 1])
        val from = points[next - 1]
        val to = points[next]
        return RoadPoint(from.lat + (to.lat - from.lat) * fraction, normalizeLongitude(from.lon + longitudeDelta(to.lon, from.lon) * fraction))
    }

    fun closestDistanceAlong(point: RoadPoint): Double {
        val longitudeScale = cos(Math.toRadians(point.lat)).coerceAtLeast(0.0001)
        var closest = Double.POSITIVE_INFINITY
        var along = 0.0
        for (index in 1 until points.size) {
            val from = points[index - 1]
            val to = points[index]
            val x = longitudeDelta(from.lon, point.lon) * longitudeScale
            val y = from.lat - point.lat
            val dx = longitudeDelta(to.lon, from.lon) * longitudeScale
            val dy = to.lat - from.lat
            val squaredLength = dx * dx + dy * dy
            if (squaredLength == 0.0) continue
            val fraction = (-(x * dx + y * dy) / squaredLength).coerceIn(0.0, 1.0)
            val distance = hypot(x + fraction * dx, y + fraction * dy)
            if (distance < closest) {
                closest = distance
                along = distances[index - 1] + (distances[index] - distances[index - 1]) * fraction
            }
        }
        return along
    }
}

internal data class RoadArrivalSample(val seconds: Double, val point: RoadPoint, val distanceMeters: Double?)
internal data class InterceptionWindow(val before: RoadArrivalSample, val after: RoadArrivalSample, val estimatedSeconds: Double)

/** Estimates assume both cars keep their measured speed and the driver stays on this road. */
sealed interface RoadInterceptionEstimate {
    data class Intercept(
        val seconds: Double,
        val distanceMeters: Double,
        val point: RoadPoint,
    ) : RoadInterceptionEstimate

    data class ArriveLater(
        val arrivalSeconds: Double,
        val secondsAfterDriver: Double,
        val distanceMeters: Double,
        val point: RoadPoint,
    ) : RoadInterceptionEstimate

    data object NotReachableAtCurrentSpeed : RoadInterceptionEstimate
    data object Unavailable : RoadInterceptionEstimate
}
