package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.MAX_POINTS_OF_INTEREST
import com.kumpello.whereiseveryone.data.model.PointOfInterest
import com.kumpello.whereiseveryone.data.network.api.PointsOfInterestApi
import com.kumpello.whereiseveryone.data.network.model.PointOfInterestRequest
import com.kumpello.whereiseveryone.data.network.model.toPointOfInterest
import java.io.IOException
import retrofit2.HttpException
import retrofit2.Response

class PointsOfInterestRepository internal constructor(private val api: PointsOfInterestApi) {
    suspend fun getOwnPoints(): List<PointOfInterest> {
        val points = api.getOwnPoints().requiredBody()
        if (points.size > MAX_POINTS_OF_INTEREST || points.map { it.id }.distinct().size != points.size) {
            throw IOException("Invalid points of interest response")
        }
        return points.map { it.toPointOfInterest(isOwn = true) }
    }

    suspend fun create(point: PointOfInterestRequest): PointOfInterest =
        api.create(point).requiredBody().toPointOfInterest(isOwn = true)

    suspend fun update(id: String, point: PointOfInterestRequest): PointOfInterest =
        api.update(id, point).requiredBody().toPointOfInterest(isOwn = true)

    suspend fun delete(id: String) {
        val response = api.delete(id)
        if (!response.isSuccessful) throw HttpException(response)
    }

    private fun <T> Response<T>.requiredBody(): T {
        if (!isSuccessful) throw HttpException(this)
        return body() ?: throw IOException("Missing points of interest response")
    }
}
