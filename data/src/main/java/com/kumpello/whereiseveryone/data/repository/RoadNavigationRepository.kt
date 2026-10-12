package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.network.api.MapboxRoadsApi
import java.io.IOException
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoadNavigationRepository internal constructor(
    private val api: MapboxRoadsApi,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val accessToken: () -> String,
) {
    suspend fun matchRoad(trace: List<LocationData>): RoadMatch? {
        val token = accessToken()
        val points = trace.takeLast(12).distinctBy { it.lastUpdate / 1_000 }
        if (token.isBlank() || points.size < 2 || points.any { !it.validCoordinate() || it.lastUpdate <= 0 }) return null
        val result = try {
            api.match(
                coordinates = points.joinToString(";") { "${it.lon},${it.lat}" },
                token = token,
                timestamps = points.joinToString(";") { (it.lastUpdate / 1_000).toString() },
                radiuses = points.joinToString(";") { (it.accuracy?.takeIf { accuracy -> accuracy.isFinite() }?.toDouble() ?: 25.0).coerceIn(5.0, 50.0).toString() },
            )
        } catch (_: IOException) {
            return null
        } catch (_: JsonDataException) {
            return null
        }
        val response = result.body()?.takeIf { result.isSuccessful && it.code == "Ok" } ?: return null
        val end = response.tracepoints.lastOrNull() ?: return null
        val matching = response.matchings.getOrNull(end.matchings_index)?.takeIf { it.confidence >= 0.5 } ?: return null
        val point = end.location.toRoadPoint() ?: return null
        val road = matching.geometry.coordinates.mapNotNull { it.toRoadPoint() }
        return RoadMatch(points.last(), point, road)
    }

    suspend fun travelEstimate(from: LocationData, to: LocationData): RoadTravelEstimate? {
        val token = accessToken()
        if (token.isBlank() || !from.validCoordinate() || !to.validCoordinate()) return null
        val result = try {
            api.route("${from.lon},${from.lat};${to.lon},${to.lat}", token)
        } catch (_: IOException) {
            return null
        } catch (_: JsonDataException) {
            return null
        }
        val response = result.body()?.takeIf { result.isSuccessful && it.code == "Ok" } ?: return null
        val route = response.routes.firstOrNull()?.takeIf {
            it.duration.isFinite() && it.duration >= 0 && it.distance.isFinite() && it.distance >= 0
        } ?: return null
        return RoadTravelEstimate(route.duration, route.distance)
    }

    suspend fun interceptionEstimate(from: LocationData, driver: LocationData, now: Long): RoadInterceptionEstimate =
        withContext(computationDispatcher) {
            val token = accessToken()
            val ownSpeed = from.speed?.toDouble()?.takeIf { it.isFinite() && it >= 0 }
            val driverSpeed = driver.speed?.toDouble()?.takeIf { it.isFinite() && it >= 0 }
            if (token.isBlank() || !from.isCarMode || !driver.isCarMode || !from.validCoordinate() ||
                !driver.validCoordinate() || ownSpeed == null || driverSpeed == null ||
                now - from.lastUpdate !in 0..10_000L || now - driver.lastUpdate !in 0..30_000L) {
                return@withContext RoadInterceptionEstimate.Unavailable
            }
            if (driverSpeed == 0.0) {
                val route = travelEstimate(from, driver) ?: return@withContext RoadInterceptionEstimate.Unavailable
                val point = RoadPoint(driver.lat, driver.lon)
                return@withContext when {
                    route.distanceMeters <= MEETING_RADIUS_METERS -> RoadInterceptionEstimate.Intercept(0.0, route.distanceMeters, point)
                    ownSpeed == 0.0 -> RoadInterceptionEstimate.NotReachableAtCurrentSpeed
                    else -> (route.distanceMeters / ownSpeed).takeIf { it.isFinite() }
                        ?.let { RoadInterceptionEstimate.Intercept(it, route.distanceMeters, point) } ?: RoadInterceptionEstimate.Unavailable
                }
            }
            val bearing = driver.bearing?.toDouble()?.takeIf { it.isFinite() && it in 0.0..360.0 }
                ?: return@withContext RoadInterceptionEstimate.Unavailable
            val fixAgeSeconds = (now - driver.lastUpdate) / 1_000.0
            val probeDistance = (driverSpeed * (INTERCEPTION_HORIZON_SECONDS + fixAgeSeconds)).coerceIn(1_000.0, INTERCEPTION_MAX_ROAD_METERS)
            val probe = RoadPoint(driver.lat, driver.lon).ahead(bearing, probeDistance)
            val path = loadForwardRoad(driver, probe, bearing, token) ?: return@withContext RoadInterceptionEstimate.Unavailable
            val horizon = (minOf(path.lengthMeters, INTERCEPTION_MAX_ROAD_METERS) / driverSpeed - fixAgeSeconds)
                .coerceAtMost(INTERCEPTION_HORIZON_SECONDS)
            // Never assume the driver stops at the end of the known road, or extrapolate across an exit.
            if (horizon <= 0) return@withContext RoadInterceptionEstimate.Unavailable
            val nearestTime = (path.closestDistanceAlong(RoadPoint(from.lat, from.lon)) / driverSpeed - fixAgeSeconds).coerceIn(0.0, horizon)
            val times = (listOf(0.0, horizon, nearestTime) + listOf(15.0, 30.0, 60.0, 120.0, 240.0, 480.0).filter { it < horizon })
                .distinct().sorted()
            val samples = roadArrivalSamples(from, path, driverSpeed, fixAgeSeconds, times, token)
                ?: return@withContext RoadInterceptionEstimate.Unavailable
            val initial = samples.first()
            if (initial.distanceMeters != null && initial.distanceMeters <= MEETING_RADIUS_METERS) {
                return@withContext RoadInterceptionEstimate.Intercept(0.0, initial.distanceMeters, initial.point)
            }
            val window = interceptionWindow(samples, ownSpeed)
            if (window != null) {
                val meeting = if (window.before.seconds == window.after.seconds) window.after else {
                    // One bounded batch validates the interpolated root and narrows the first crossing.
                    // Select a point actually reachable before the driver; never trust interpolation alone.
                    val refinedTimes = ((0..8).map { window.before.seconds + (window.after.seconds - window.before.seconds) * it / 8 } + window.estimatedSeconds)
                        .distinct().sorted()
                    val refined = roadArrivalSamples(from, path, driverSpeed, fixAgeSeconds, refinedTimes, token)
                    refined?.let { interceptionWindow(it, ownSpeed)?.after } ?: window.after
                }
                return@withContext RoadInterceptionEstimate.Intercept(meeting.seconds, requireNotNull(meeting.distanceMeters), meeting.point)
            }
            if (ownSpeed == 0.0) return@withContext RoadInterceptionEstimate.NotReachableAtCurrentSpeed
            val projected = samples.lastOrNull { it.seconds > 0 && it.distanceMeters != null }
                ?: return@withContext RoadInterceptionEstimate.Unavailable
            val distance = requireNotNull(projected.distanceMeters)
            val arrival = distance / ownSpeed
            if (!arrival.isFinite()) return@withContext RoadInterceptionEstimate.NotReachableAtCurrentSpeed
            RoadInterceptionEstimate.ArriveLater(arrival, (arrival - projected.seconds).coerceAtLeast(0.0), distance, projected.point)
        }

    private suspend fun loadForwardRoad(fix: LocationData, probe: RoadPoint, bearing: Double, token: String): ForwardRoadPath? {
        val radius = (fix.accuracy?.toDouble()?.takeIf { it.isFinite() } ?: 25.0).coerceIn(10.0, 50.0)
        val result = try {
            api.forwardRoad("${fix.lon},${fix.lat};${probe.lon},${probe.lat}", token, "$bearing,45;", "$radius;unlimited")
        } catch (_: IOException) {
            return null
        } catch (_: JsonDataException) {
            return null
        }
        return result.body()?.takeIf { result.isSuccessful && it.code == "Ok" }?.routes?.firstOrNull()?.currentRoad(fix, bearing)
    }

    private suspend fun roadArrivalSamples(
        from: LocationData,
        path: ForwardRoadPath,
        driverSpeed: Double,
        fixAgeSeconds: Double,
        times: List<Double>,
        token: String,
    ): List<RoadArrivalSample>? {
        val points = times.map { path.pointAt((driverSpeed * (fixAgeSeconds + it)).coerceIn(0.0, path.lengthMeters)) }
        val heading = from.bearing?.toDouble()?.takeIf { it.isFinite() && it in 0.0..360.0 && (from.speed ?: 0f) >= 1f }
        val result = try {
            api.distances(
                coordinates = (listOf(RoadPoint(from.lat, from.lon)) + points).joinToString(";") { "${it.lon},${it.lat}" },
                token = token,
                destinations = (1..points.size).joinToString(";"),
                bearings = heading?.let { "$it,45" + ";".repeat(points.size) },
            )
        } catch (_: IOException) {
            return null
        } catch (_: JsonDataException) {
            return null
        }
        val distances = result.body()?.takeIf { result.isSuccessful && it.code == "Ok" }?.distances?.singleOrNull()
            ?.takeIf { it.size == points.size } ?: return null
        return times.indices.map { index ->
            RoadArrivalSample(times[index], points[index], distances[index]?.takeIf { it.isFinite() && it >= 0 })
        }
    }

    private fun LocationData.validCoordinate() = lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0

    private fun List<Double>.toRoadPoint(): RoadPoint? {
        if (size < 2 || !this[0].isFinite() || !this[1].isFinite() || this[0] !in -180.0..180.0 || this[1] !in -90.0..90.0) return null
        return RoadPoint(lat = this[1], lon = this[0])
    }
}

data class RoadPoint(val lat: Double, val lon: Double)
data class RoadMatch(val fix: LocationData, val point: RoadPoint, val road: List<RoadPoint>)
data class RoadTravelEstimate(val durationSeconds: Double, val distanceMeters: Double)
