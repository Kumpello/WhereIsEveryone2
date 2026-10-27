package com.kumpello.whereiseveryone.data.network.api

import com.kumpello.whereiseveryone.data.network.model.PointOfInterestData
import com.kumpello.whereiseveryone.data.network.model.PointOfInterestRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface PointsOfInterestApi {
    @GET("me/points")
    suspend fun getOwnPoints(): Response<List<PointOfInterestData>>

    @POST("me/points")
    suspend fun create(@Body point: PointOfInterestRequest): Response<PointOfInterestData>

    @PUT("me/points/{id}")
    suspend fun update(@Path("id") id: String, @Body point: PointOfInterestRequest): Response<PointOfInterestData>

    @DELETE("me/points/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
