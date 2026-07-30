package com.kumpello.whereiseveryone.main.common.domain.manager

import android.location.Location
import app.cash.turbine.test
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.main.map.domain.model.FriendData
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import com.kumpello.whereiseveryone.main.map.domain.model.UserInfo
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProximityManagerTest {

    private val locationService: LocationService = mockk()
    private val friendsManager: FriendsManager = mockk()
    private val preferencesManager: PreferencesManager = mockk()
    private val proximityManager = ProximityManager(locationService, friendsManager, preferencesManager)

    @Test
    fun `observeNearbyFriends filters friends based on threshold`() = runTest {
        val userLocation = mockk<Location> {
            every { latitude } returns 0.0
            every { longitude } returns 0.0
            every { altitude } returns 0.0
        }
        
        // Approx 111km away (1 deg lat)
        val farFriend = FriendData("far", "", "accepted", UserInfo(1.0, 0.0, 0f, 0.0, 0f, 0f, 0L), 0L)
        // Same spot
        val nearFriend = FriendData("near", "", "accepted", UserInfo(0.0, 0.0, 0f, 0.0, 0f, 0f, 0L), 0L)
        
        val friendsResponse = FriendsResponse.FriendsData(listOf(farFriend, nearFriend))

        every { locationService.observeLocation() } returns MutableStateFlow(userLocation)
        every { friendsManager.observeFriends() } returns flowOf(friendsResponse)
        every { preferencesManager.observe(PreferencesKey.ProximityDistance) } returns flowOf(1000) // 1km threshold

        proximityManager.observeNearbyFriends().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("near", result[0])
            cancelAndIgnoreRemainingEvents()
        }
    }
}
