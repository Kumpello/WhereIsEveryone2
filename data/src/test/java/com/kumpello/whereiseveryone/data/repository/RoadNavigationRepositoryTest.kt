package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.network.api.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RoadNavigationRepositoryTest {
    private val api = mockk<MapboxRoadsApi>()
    private val repository = RoadNavigationRepository(api) { "test-token" }
    private val fix = LocationData(52.0, 21.0, 90f, null, 10f, 10f, 1_800_000_000_000L, true)
    private val trace = listOf(fix.copy(lon = 20.999, lastUpdate = fix.lastUpdate - 5_000), fix)

    @Test
    fun `match uses driving road point and rejects unmatched or uncertain traces`() = runTest {
        val matching = RoadMatchingResponse("Ok", listOf(null, RoadTracePoint(listOf(21.00001, 52.0), 0)),
            listOf(RoadMatching(0.9, RoadGeometry(listOf(listOf(20.999, 52.0), listOf(21.00001, 52.0))))))
        coEvery { api.match(any(), any(), any(), any(), any(), any()) } returns Response.success(matching)
        val match = requireNotNull(repository.matchRoad(trace))
        assertEquals(21.00001, match.point.lon, 0.0)
        assertEquals(fix, match.fix)
        coEvery { api.match(any(), any(), any(), any(), any(), any()) } returns Response.success(matching.copy(tracepoints = listOf(null, null)))
        assertNull(repository.matchRoad(trace))
        coEvery { api.match(any(), any(), any(), any(), any(), any()) } returns Response.success(matching.copy(matchings = listOf(matching.matchings.single().copy(confidence = 0.1))))
        assertNull(repository.matchRoad(trace))
    }

    @Test
    fun `invalid and single point traces never issue a request`() = runTest {
        assertNull(repository.matchRoad(listOf(fix)))
        assertNull(repository.matchRoad(trace.map { it.copy(lat = Double.NaN) }))
        coVerify(exactly = 0) { api.match(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `route duration comes from roads and network failure returns unavailable`() = runTest {
        coEvery { api.route(any(), any(), any()) } returns Response.success(DrivingRouteResponse("Ok", listOf(DrivingRoute(900.0, 12000.0))))
        assertEquals(RoadTravelEstimate(900.0, 12000.0), repository.travelEstimate(trace.first(), fix))
        coEvery { api.route(any(), any(), any()) } throws IOException("offline")
        assertNull(repository.travelEstimate(trace.first(), fix))
        coEvery { api.route(any(), any(), any()) } returns Response.success(DrivingRouteResponse("Ok", listOf(DrivingRoute(Double.NaN, 12.0))))
        assertNull(repository.travelEstimate(trace.first(), fix))
    }
}
