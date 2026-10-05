package com.kumpello.whereiseveryone.main.map.domain.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FriendDataTest {
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
