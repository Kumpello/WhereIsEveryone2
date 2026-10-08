package com.kumpello.whereiseveryone.feature.main.ui.mapper

import com.kumpello.whereiseveryone.feature.main.ui.model.AltDifference
import com.kumpello.whereiseveryone.data.model.FriendLocalData
import com.kumpello.whereiseveryone.data.model.FriendState
import com.kumpello.whereiseveryone.data.model.LocationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapFriendUseCaseTest {

    private val useCase = MapFriendUseCase()

    @Test
    fun `execute maps FriendLocalData to Friend correctly`() {
        val lastUpdate = System.currentTimeMillis()
        val friendLocalData = FriendLocalData(
            username = "friend1",
            status = "status",
            state = FriendState.ACCEPTED,
            location = LocationData(1.0, 2.0, 0f, 3.0, 4f, 10.0f, lastUpdate),
            friendSince = lastUpdate,
        )
        val userLocation = LocationData(0.0, 0.0, 0f, 0.0, 0f, 0f, lastUpdate)

        val result = useCase.execute(friendLocalData, userLocation)

        assertEquals("friend1", result.username)
        // Values are now calculated by LocationUtils, we can check if they are not null
        // or we could check specific values if we know the expected behavior of LocationUtils
        assertEquals(AltDifference.SOMEWHAT_SAME, result.location?.alt)
        assertEquals(10.0f, result.location?.speed)
    }

    @Test
    fun `execute handles null friend location`() {
        val lastUpdate = System.currentTimeMillis()
        val friendLocalData = FriendLocalData(
            username = "friend1",
            status = "status",
            state = FriendState.ACCEPTED,
            location = null,
            friendSince = null
        )
        val userLocation = LocationData(0.0, 0.0, 0f, 0.0, 0f, 0f, lastUpdate)

        val result = useCase.execute(friendLocalData, userLocation)

        assertEquals("friend1", result.username)
        assertNull(result.location)
        assertNull(result.distance)
    }
}
