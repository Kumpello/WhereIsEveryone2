package com.kumpello.whereiseveryone.feature.main.ui.map

import androidx.compose.runtime.mutableStateMapOf
import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendFeatureUpdatesTest {
    private val position = AnimatedFriendData(
        lat = 52.2296756,
        lon = 21.0122287,
        bearing = 359.0,
        opacity = 0.75,
        haloWidth = 1.5,
        speed = 2.0,
    )

    @Test
    fun `initial feature has stable ID and retains precise coordinates and styling`() {
        val updates = FriendFeatureUpdates { 0xFF012345.toInt() }
        val changes = updates.update(mapOf("alice" to position))
        val feature = changes.added.single()
        val point = feature.geometry() as Point
        assertEquals("alice", feature.id())
        assertEquals(position.lat, point.latitude(), 0.0)
        assertEquals(position.lon, point.longitude(), 0.0)
        assertEquals("alice", feature.getStringProperty("id"))
        assertEquals("avatar-alice", feature.getStringProperty("avatarId"))
        assertEquals("#012345", feature.getStringProperty("color"))
        assertEquals(position.bearing, feature.getNumberProperty("bearing").toDouble(), 0.0)
        assertEquals(position.opacity, feature.getNumberProperty("opacity").toDouble(), 0.0)
        assertEquals(position.haloWidth, feature.getNumberProperty("haloWidth").toDouble(), 0.0)
        assertEquals(position.speed, feature.getNumberProperty("speed").toDouble(), 0.0)
        assertTrue(changes.updated.isEmpty())
        assertTrue(changes.removed.isEmpty())
    }

    @Test
    fun `identical snapshot submits no work`() {
        val updates = FriendFeatureUpdates { 0 }
        updates.update(mapOf("alice" to position))
        val changes = updates.update(mapOf("alice" to position.copy()))
        assertTrue(changes.added.isEmpty())
        assertTrue(changes.updated.isEmpty())
        assertTrue(changes.removed.isEmpty())
    }

    @Test
    fun `animation state snapshots stay immutable while the live map changes`() {
        val state = mutableStateMapOf("alice" to position)
        val snapshot = state.toMap()
        val updates = FriendFeatureUpdates { 0 }
        updates.update(snapshot)
        state["alice"] = position.copy(speed = 3.0)
        assertEquals(position, snapshot.getValue("alice"))
        val changes = updates.update(state.toMap())
        assertEquals("alice", changes.updated.single().id())
        assertEquals(3.0, changes.updated.single().getNumberProperty("speed").toDouble(), 0.0)
    }

    @Test
    fun `one moving friend among a thousand rebuilds only one feature`() {
        var colorComputations = 0
        val updates = FriendFeatureUpdates { colorComputations++; 0 }
        val friends = (1..1000).associate { "friend-$it" to position }
        assertEquals(1000, updates.update(friends).added.size)

        val changes = updates.update(friends + ("friend-500" to position.copy(lat = position.lat + 0.000001)))
        assertEquals("friend-500", changes.updated.single().id())
        assertTrue(changes.added.isEmpty())
        assertTrue(changes.removed.isEmpty())
        assertEquals(1000, colorComputations)
    }

    @Test
    fun `adding removing and updating friends are separate operations`() {
        val updates = FriendFeatureUpdates { 0 }
        updates.update(mapOf("alice" to position, "bob" to position))
        val changes = updates.update(mapOf(
            "alice" to position.copy(opacity = 0.3),
            "charlie" to position,
        ))
        assertEquals("charlie", changes.added.single().id())
        assertEquals("alice", changes.updated.single().id())
        assertEquals(listOf("bob"), changes.removed)
    }

    @Test
    fun `previously submitted features are not mutated by later animation frames`() {
        val updates = FriendFeatureUpdates { 0 }
        val first = updates.update(mapOf("alice" to position)).added.single()
        val second = updates.update(mapOf("alice" to position.copy(bearing = 1.0))).updated.single()
        assertEquals(359.0, first.getNumberProperty("bearing").toDouble(), 0.0)
        assertEquals(1.0, second.getNumberProperty("bearing").toDouble(), 0.0)
    }

    @Test
    fun `clearing friends removes features and releases their cached colors`() {
        var colorComputations = 0
        val updates = FriendFeatureUpdates { colorComputations++; 0 }
        updates.update(mapOf("alice" to position))
        assertEquals(listOf("alice"), updates.update(emptyMap()).removed)
        assertEquals("alice", updates.update(mapOf("alice" to position)).added.single().id())
        assertEquals(2, colorComputations)
    }
}
