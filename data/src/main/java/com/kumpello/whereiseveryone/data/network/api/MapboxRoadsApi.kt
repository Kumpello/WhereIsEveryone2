package com.kumpello.whereiseveryone.data.network.api

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface MapboxRoadsApi {
    @GET("matching/v5/mapbox/driving/{coordinates}")
    suspend fun match(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("access_token") token: String,
        @Query("timestamps") timestamps: String,
        @Query("radiuses") radiuses: String,
        @Query("geometries") geometries: String = "geojson",
        @Query("overview") overview: String = "full",
    ): Response<RoadMatchingResponse>

    @GET("directions/v5/mapbox/driving-traffic/{coordinates}")
    suspend fun route(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("access_token") token: String,
        @Query("overview") overview: String = "false",
    ): Response<DrivingRouteResponse>

    @GET("directions/v5/mapbox/driving/{coordinates}")
    suspend fun forwardRoad(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("access_token") token: String,
        @Query("bearings") bearings: String,
        @Query("radiuses") radiuses: String,
        @Query("steps") steps: Boolean = true,
        @Query("geometries") geometries: String = "geojson",
        @Query("overview") overview: String = "full",
    ): Response<DrivingRouteResponse>

    @GET("directions-matrix/v1/mapbox/driving/{coordinates}")
    suspend fun distances(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("access_token") token: String,
        @Query("destinations") destinations: String,
        @Query("bearings") bearings: String?,
        @Query("sources") sources: String = "0",
        @Query("annotations") annotations: String = "distance",
    ): Response<RoadDistanceResponse>
}

@JsonClass(generateAdapter = true)
internal data class RoadMatchingResponse(
    val code: String,
    val tracepoints: List<RoadTracePoint?> = emptyList(),
    val matchings: List<RoadMatching> = emptyList(),
)

@JsonClass(generateAdapter = true)
internal data class RoadTracePoint(val location: List<Double>, val matchings_index: Int)

@JsonClass(generateAdapter = true)
internal data class RoadMatching(val confidence: Double, val geometry: RoadGeometry)

@JsonClass(generateAdapter = true)
internal data class RoadGeometry(val coordinates: List<List<Double>>)

@JsonClass(generateAdapter = true)
internal data class DrivingRouteResponse(val code: String, val routes: List<DrivingRoute> = emptyList())

@JsonClass(generateAdapter = true)
internal data class DrivingRoute(
    val duration: Double,
    val distance: Double,
    val legs: List<DrivingLeg> = emptyList(),
)

@JsonClass(generateAdapter = true)
internal data class DrivingLeg(val steps: List<DrivingStep> = emptyList())

@JsonClass(generateAdapter = true)
internal data class DrivingStep(
    val maneuver: RoadManeuver,
    val geometry: RoadGeometry,
    val name: String = "",
    val ref: String = "",
    val mode: String = "driving",
)

@JsonClass(generateAdapter = true)
internal data class RoadManeuver(
    val type: String,
    val modifier: String = "",
    val bearing_after: Double = 0.0,
)

@JsonClass(generateAdapter = true)
internal data class RoadDistanceResponse(
    val code: String,
    val distances: List<List<Double?>> = emptyList(),
)
