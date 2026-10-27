package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.model.isValidPointText
import com.kumpello.whereiseveryone.data.model.toLocalData
import com.kumpello.whereiseveryone.data.network.api.PointsOfInterestApi
import com.kumpello.whereiseveryone.data.network.model.FriendData
import com.kumpello.whereiseveryone.data.network.model.PointOfInterestData
import com.kumpello.whereiseveryone.data.network.model.PointOfInterestRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PointsOfInterestRepositoryTest {
    private val api = mockk<PointsOfInterestApi>()
    private val repository = PointsOfInterestRepository(api)
    private val point = PointOfInterestData("point", "Bridge", "Meet here", 0.0, 0.0, "alice", 1_800_000_000_000)

    @Test
    fun `own list and mutations preserve server metadata and ownership`() = runTest {
        coEvery { api.getOwnPoints() } returns Response.success(listOf(point))
        coEvery { api.create(any()) } returns Response.success(point)
        coEvery { api.update(any(), any()) } returns Response.success(point.copy(name = "Updated"))
        coEvery { api.delete(any()) } returns Response.success(Unit)
        val own = repository.getOwnPoints().single()
        assertTrue(own.isOwn)
        assertEquals("alice", own.creatorUsername)
        assertEquals(point.createdAt, own.createdAt)
        val request = PointOfInterestRequest("Bridge", "Meet here", 0.0, 0.0)
        assertEquals(own, repository.create(request))
        assertEquals("Updated", repository.update(point.id, request).name)
        repository.delete(point.id)
        coVerify(exactly = 1) { api.delete(point.id) }
    }

    @Test(expected = HttpException::class)
    fun `server point limit is propagated instead of reporting a saved point`() = runTest {
        coEvery { api.create(any()) } returns Response.error(409, "{}".toResponseBody())
        repository.create(PointOfInterestRequest("Sixth", "", 52.0, 21.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid coordinates never reach map state`() = runTest {
        coEvery { api.getOwnPoints() } returns Response.success(listOf(point.copy(latitude = Double.NaN)))
        repository.getOwnPoints()
    }

    @Test
    fun `friend points map without a live location`() {
        val adapter = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter(FriendData::class.java)
        val accepted = requireNotNull(adapter.fromJson("""
            {"username":"bob","state":"accepted","friend_since":null,"points_of_interest":[
              {"id":"point","name":"Bridge","description":"Meet here","latitude":0,"longitude":0,
               "creator_username":"bob","created_at":1800000000000}]}
        """.trimIndent()))
        val shared = accepted.toLocalData().pointsOfInterest.single()
        assertFalse(shared.isOwn)
        assertEquals("bob", shared.creatorUsername)
        assertEquals(1_800_000_000_000, shared.createdAt)
    }

    @Test
    fun `point text limits count Unicode characters consistently with the server`() {
        assertTrue(isValidPointText("😀".repeat(64), "😀".repeat(1000)))
        assertFalse(isValidPointText("😀".repeat(65), ""))
        assertFalse(isValidPointText("Bridge", "😀".repeat(1001)))
        assertFalse(isValidPointText("   ", "Description"))
    }
}
