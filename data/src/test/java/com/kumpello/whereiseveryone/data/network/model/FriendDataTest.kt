package com.kumpello.whereiseveryone.data.network.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.kumpello.whereiseveryone.data.local.database.toDatabaseEntity
import com.kumpello.whereiseveryone.data.local.database.toDomain
import com.kumpello.whereiseveryone.data.model.toLocalData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendDataTest {
    @Test
    fun `speed upload omits unavailable measurement but includes measured zero`() {
        val adapter = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            .adapter(LocationRequest::class.java)
        val unknown = LocationRequest(21.0, 52.0, null, null, 5f, null, 1800000000000L, true)
        val unknownJson = adapter.toJson(unknown)
        assertFalse(unknownJson.contains("\"speed\":"))
        assertNull(adapter.fromJson(unknownJson)?.speed)

        val stoppedJson = adapter.toJson(unknown.copy(speed = 0f))
        assertTrue(stoppedJson.contains("\"speed\":"))
        assertEquals(0f, adapter.fromJson(stoppedJson)?.speed)
    }

    @Test
    fun `friends preserve GPS course including north and missing measurements through wire and cache`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val friendAdapter = moshi.adapter(FriendData::class.java)
        val requestAdapter = moshi.adapter(LocationRequest::class.java)
        for (carMode in listOf(false, true)) {
            for (course in listOf(null, 0f, 90f, 359.5f)) {
                val request = LocationRequest(21.0, 52.0, course, null, 5f, 15f, 1800000000000L, carMode)
                assertEquals(course, requestAdapter.fromJson(requestAdapter.toJson(request))?.bearing)
                val encodedCourse = course?.toString() ?: "null"
                val friend = requireNotNull(friendAdapter.fromJson("""
                    {"username":"driver","state":"accepted","status":"","friend_since":null,
                     "location":{"latitude":52,"longitude":21,"bearing":$encodedCourse,
                     "speed":15,"accuracy":5,"last_update":1800000000000,"car_mode":$carMode}}
                """.trimIndent()))
                val cached = friend.toDatabaseEntity().toDomain().toLocalData().location
                assertEquals(course, cached?.bearing)
                assertEquals(carMode, cached?.isCarMode)
            }
        }
    }

    @Test
    fun `car mode boolean survives wire models and friend cache`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val requestAdapter = moshi.adapter(LocationRequest::class.java)
        for (enabled in listOf(false, true)) {
            val request = LocationRequest(21.0, 52.0, null, null, null, 0f, 1800000000000L, enabled)
            val json = requestAdapter.toJson(request)
            org.junit.Assert.assertTrue(json.contains("\"car_mode\":$enabled"))
            assertEquals(request, requestAdapter.fromJson(json))
        }
        val friend = FriendData("driver", "", "accepted",
            UserInfo(52.0, 21.0, null, null, 5f, 0f, 1800000000000L, true), null)
        val cached = friend.toDatabaseEntity().toDomain()
        assertEquals(friend, cached)
        assertEquals(true, cached.toLocalData().location?.isCarMode)
    }
    private val adapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<FriendData>>(Types.newParameterizedType(List::class.java, FriendData::class.java))

    @Test
    fun `paginated response preserves opaque cursor and pending privacy`() {
        val page = requireNotNull(Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            .adapter(FriendsPage::class.java).fromJson("""
                {"items":[{"username":"incoming","state":"pending_incoming","friend_since":null}],
                 "next_cursor":"opaque-next-page"}
            """.trimIndent()))

        assertEquals("opaque-next-page", page.next_cursor)
        assertEquals("", page.items.single().status)
        assertNull(page.items.single().location)
        assertNull(page.items.single().friend_since)
    }

    @Test
    fun `friend list accepts pending entries without private status or location`() {
        val friends = requireNotNull(adapter.fromJson("""
            [
                {"username":"accepted","status":"On my way","state":"accepted","friend_since":1700000000000},
                {"username":"incoming","state":"pending_incoming","friend_since":null},
                {"username":"outgoing","state":"pending_outgoing","friend_since":null}
            ]
        """.trimIndent()))

        assertEquals(3, friends.size)
        assertEquals("On my way", friends[0].status)
        assertEquals(1700000000000L, friends[0].friend_since)
        assertEquals("", friends[1].status)
        assertEquals("pending_incoming", friends[1].state)
        assertEquals("", friends[2].status)
        assertEquals("pending_outgoing", friends[2].state)
        friends.drop(1).forEach {
            assertNull(it.location)
            assertNull(it.friend_since)
        }
    }

    @Test
    fun `accepted friend preserves explicitly empty status`() {
        val friends = requireNotNull(adapter.fromJson("""
            [{"username":"accepted","status":"","state":"accepted","friend_since":1700000000000}]
        """.trimIndent()))

        assertEquals("", friends.single().status)
    }
}
