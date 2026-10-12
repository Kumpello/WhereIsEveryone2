package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.network.api.DrivingLeg
import com.kumpello.whereiseveryone.data.network.api.DrivingRoute
import com.kumpello.whereiseveryone.data.network.api.DrivingRouteResponse
import com.kumpello.whereiseveryone.data.network.api.DrivingStep
import com.kumpello.whereiseveryone.data.network.api.MapboxRoadsApi
import com.kumpello.whereiseveryone.data.network.api.RoadDistanceResponse
import com.kumpello.whereiseveryone.data.network.api.RoadGeometry
import com.kumpello.whereiseveryone.data.network.api.RoadManeuver
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import kotlin.math.abs

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoadInterceptionTest {
    private val api = mockk<MapboxRoadsApi>()
    private val now = 1_800_000_000_000L
    private val metersPerDegree = Math.PI * 6_371_000.0 / 180.0
    private val driver = LocationData(0.0, 0.0, 90f, null, 10f, 10f, now, true)
    private val follower = driver.copy(lon = -1_000 / metersPerDegree, speed = 20f)

    @Test
    fun `faster follower intercepts using closing speed rather than distance to last fix`() = runTest {
        straightRoad()
        directDistances()
        val estimate = repository().interceptionEstimate(follower, driver, now) as RoadInterceptionEstimate.Intercept
        assertEquals(100.0, estimate.seconds, 0.01) // 1000 / (20 - 10), not 1000 / 20.
        assertEquals(2_000.0, estimate.distanceMeters, 0.1)
        assertEquals(1_000.0 / metersPerDegree, estimate.point.lon, 0.000001)
        coVerify(exactly = 1) { api.forwardRoad(any(), "test-token", "90.0,45;", "10.0;unlimited", true, "geojson", "full") }
        coVerify(exactly = 2) { api.distances(any(), any(), any(), any(), "0", "distance") }
        coVerify(exactly = 0) { api.route(any(), any(), any()) }
    }

    @Test
    fun `approaching cars meet using the sum of their speeds`() = runTest {
        straightRoad()
        directDistances()
        val approaching = follower.copy(lon = 1_000 / metersPerDegree, bearing = 270f, speed = 30f)
        val estimate = repository().interceptionEstimate(approaching, driver, now) as RoadInterceptionEstimate.Intercept
        assertEquals(25.0, estimate.seconds, 0.01) // 1000 / (30 + 10).
        assertEquals(750.0, estimate.distanceMeters, 0.1)
    }

    @Test
    fun `equal and slower following speeds give finite arrival lateness instead of an interception`() = runTest {
        straightRoad()
        directDistances()
        val repository = repository()
        val equal = repository.interceptionEstimate(follower.copy(speed = 10f), driver, now) as RoadInterceptionEstimate.ArriveLater
        assertEquals(1_000.0, equal.arrivalSeconds, 0.01)
        assertEquals(100.0, equal.secondsAfterDriver, 0.01)
        val slower = repository.interceptionEstimate(follower.copy(speed = 5f), driver, now) as RoadInterceptionEstimate.ArriveLater
        assertEquals(2_000.0, slower.arrivalSeconds, 0.01)
        assertEquals(1_100.0, slower.secondsAfterDriver, 0.01)
    }

    @Test
    fun `prediction follows road bends instead of projecting a straight heading`() = runTest {
        road(step("depart", listOf(listOf(0.0, 0.0), listOf(0.005, 0.0), listOf(0.005, 0.2))))
        coEvery { api.distances(any(), any(), any(), any(), any(), any()) } answers {
            val coordinates = firstArg<String>().split(';').map { it.split(',').map(String::toDouble) }
            Response.success(RoadDistanceResponse("Ok", listOf(coordinates.drop(1).map { 1_000 + (it[0] + it[1]) * metersPerDegree })))
        }
        val estimate = repository().interceptionEstimate(follower, driver, now) as RoadInterceptionEstimate.Intercept
        assertEquals(100.0, estimate.seconds, 0.1)
        assertEquals(0.005, estimate.point.lon, 0.000001)
        assertTrue(estimate.point.lat > 0)
    }

    @Test
    fun `age of the driver fix advances their position before calculating an interception`() = runTest {
        straightRoad()
        directDistances()
        val estimate = repository().interceptionEstimate(follower, driver.copy(lastUpdate = now - 20_000), now) as RoadInterceptionEstimate.Intercept
        assertEquals(120.0, estimate.seconds, 0.01)
        assertEquals(2_400.0, estimate.distanceMeters, 0.1)
    }

    @Test
    fun `stationary driver uses own measured speed and stationary follower can wait for a passing driver`() = runTest {
        coEvery { api.route(any(), any(), any()) } returns Response.success(DrivingRouteResponse("Ok", listOf(DrivingRoute(500.0, 1_000.0))))
        val repository = repository()
        val stoppedDriver = repository.interceptionEstimate(follower, driver.copy(speed = 0f, bearing = null), now) as RoadInterceptionEstimate.Intercept
        assertEquals(50.0, stoppedDriver.seconds, 0.0)
        assertEquals(RoadInterceptionEstimate.NotReachableAtCurrentSpeed, repository.interceptionEstimate(follower.copy(speed = 0f), driver.copy(speed = 0f), now))
        straightRoad()
        directDistances()
        val waiting = repository.interceptionEstimate(follower.copy(lon = 1_000 / metersPerDegree, speed = 0f, bearing = null), driver, now) as RoadInterceptionEstimate.Intercept
        assertTrue(waiting.seconds in 98.0..100.01)
        assertTrue(waiting.distanceMeters <= MEETING_RADIUS_METERS + 0.01)
    }

    @Test
    fun `prediction stops before a turn or exit and never assumes a stop at the geometry endpoint`() = runTest {
        val boundary = 100 / metersPerDegree
        road(
            step("depart", listOf(listOf(0.0, 0.0), listOf(boundary, 0.0))),
            step("off ramp", listOf(listOf(boundary, 0.0), listOf(0.2, 0.0))),
        )
        directDistances()
        val repository = repository()
        val late = repository.interceptionEstimate(follower, driver, now) as RoadInterceptionEstimate.ArriveLater
        assertEquals(55.0, late.arrivalSeconds, 0.01)
        assertEquals(45.0, late.secondsAfterDriver, 0.01)
        assertEquals(boundary, late.point.lon, 0.000001)
        assertEquals(RoadInterceptionEstimate.Unavailable, repository.interceptionEstimate(follower, driver.copy(lastUpdate = now - 20_000), now))
    }

    @Test
    fun `refinement verifies real road reachability before accepting an interpolated time`() = runTest {
        straightRoad()
        var batches = 0
        coEvery { api.distances(any(), any(), any(), any(), any(), any()) } answers {
            batches++
            val coordinates = firstArg<String>().split(';').map { it.split(',').map(String::toDouble) }
            val distances = coordinates.drop(1).map {
                val distance = (it[0] - coordinates.first()[0]) * metersPerDegree
                val seconds = it[0] * metersPerDegree / 10.0
                distance + if (batches > 1 && seconds < 110.0) 500.0 else 0.0 // A detour missed by the coarse batch.
            }
            Response.success(RoadDistanceResponse("Ok", listOf(distances)))
        }
        val estimate = repository().interceptionEstimate(follower, driver, now) as RoadInterceptionEstimate.Intercept
        assertTrue(estimate.seconds >= 110)
        assertTrue(estimate.distanceMeters / 20.0 <= estimate.seconds)
        assertEquals(2, batches)
    }

    @Test
    fun `missing invalid future and stale measurements do not start prediction requests`() = runTest {
        val repository = repository()
        for (bad in listOf(driver.copy(speed = null), driver.copy(speed = Float.NaN), driver.copy(speed = -1f),
            driver.copy(bearing = null), driver.copy(lastUpdate = now + 1), driver.copy(lastUpdate = now - 30_001),
            driver.copy(lat = Double.NaN), driver.copy(isCarMode = false))) {
            assertEquals(RoadInterceptionEstimate.Unavailable, repository.interceptionEstimate(follower, bad, now))
        }
        assertEquals(RoadInterceptionEstimate.Unavailable, repository.interceptionEstimate(follower.copy(lastUpdate = now - 10_001), driver, now))
        coVerify(exactly = 0) { api.forwardRoad(any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { api.distances(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `offline malformed and unreachable road distances remain unavailable`() = runTest {
        straightRoad()
        val repository = repository()
        coEvery { api.distances(any(), any(), any(), any(), any(), any()) } throws IOException("offline")
        assertEquals(RoadInterceptionEstimate.Unavailable, repository.interceptionEstimate(follower, driver, now))
        coEvery { api.distances(any(), any(), any(), any(), any(), any()) } returns Response.success(RoadDistanceResponse("Ok", listOf(listOf(1.0))))
        assertEquals(RoadInterceptionEstimate.Unavailable, repository.interceptionEstimate(follower, driver, now))
        coEvery { api.distances(any(), any(), any(), any(), any(), any()) } answers {
            val count = thirdArg<String>().split(';').size
            Response.success(RoadDistanceResponse("Ok", listOf(List(count) { null })))
        }
        assertEquals(RoadInterceptionEstimate.Unavailable, repository.interceptionEstimate(follower, driver, now))
    }

    @Test
    fun `wrong direction far snapping and disconnected geometry do not create a road prediction`() = runTest {
        val repository = repository()
        straightRoad()
        assertEquals(RoadInterceptionEstimate.Unavailable, repository.interceptionEstimate(follower, driver.copy(bearing = 270f), now))
        road(step("depart", listOf(listOf(0.1, 0.0), listOf(0.2, 0.0))))
        assertEquals(RoadInterceptionEstimate.Unavailable, repository.interceptionEstimate(follower, driver, now))
        road(
            step("depart", listOf(listOf(0.0, 0.0), listOf(0.001, 0.0))),
            step("new name", listOf(listOf(0.002, 0.0), listOf(0.2, 0.0))),
        )
        assertEquals(RoadInterceptionEstimate.Unavailable, repository.interceptionEstimate(follower, driver, now))
        coVerify(exactly = 0) { api.distances(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `cancelling prediction cancels the road request instead of calculating in background`() = runTest {
        coEvery { api.forwardRoad(any(), any(), any(), any(), any(), any(), any()) } coAnswers { awaitCancellation() }
        val repository = repository()
        val job = launch { repository.interceptionEstimate(follower, driver, now) }
        runCurrent()
        job.cancelAndJoin()
        coVerify(exactly = 1) { api.forwardRoad(any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { api.distances(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Mapbox geometry steps and nullable distance pairs deserialize`() {
        val moshi = Moshi.Builder().build()
        val road = moshi.adapter(DrivingRouteResponse::class.java).fromJson(
            """{"code":"Ok","routes":[{"duration":120,"distance":1000,"legs":[{"steps":[{"name":"Main road","maneuver":{"type":"depart","bearing_after":90},"geometry":{"type":"LineString","coordinates":[[0,0],[0.01,0]]}}]}]}]}"""
        )
        assertEquals("Main road", road!!.routes.single().legs.single().steps.single().name)
        val matrix = moshi.adapter(RoadDistanceResponse::class.java).fromJson("""{"code":"Ok","distances":[[100,null,300]]}""")
        assertEquals(listOf(100.0, null, 300.0), matrix!!.distances.single())
    }
    private fun TestScope.repository() = RoadNavigationRepository(api, StandardTestDispatcher(testScheduler)) { "test-token" }

    private fun step(type: String, points: List<List<Double>>, name: String = "Main road", modifier: String = "") =
        DrivingStep(RoadManeuver(type, modifier, 90.0), RoadGeometry(points), name)

    private fun road(vararg steps: DrivingStep) {
        coEvery { api.forwardRoad(any(), any(), any(), any(), any(), any(), any()) } returns Response.success(
            DrivingRouteResponse("Ok", listOf(DrivingRoute(1_000.0, 20_000.0, listOf(DrivingLeg(steps.toList())))))
        )
    }

    private fun straightRoad() = road(step("depart", listOf(listOf(0.0, 0.0), listOf(0.2, 0.0))))

    /** A bidirectional road along the equator, with exact distances independent of the solver. */
    private fun directDistances() {
        coEvery { api.distances(any(), any(), any(), any(), any(), any()) } answers {
            val coordinates = firstArg<String>().split(';').map { it.split(',').map(String::toDouble) }
            val origin = coordinates.first()[0]
            Response.success(RoadDistanceResponse("Ok", listOf(coordinates.drop(1).map { abs(it[0] - origin) * metersPerDegree })))
        }
    }


}
