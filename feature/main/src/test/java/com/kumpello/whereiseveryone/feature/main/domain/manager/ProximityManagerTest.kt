package com.kumpello.whereiseveryone.feature.main.domain.manager

import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import android.location.Location
import app.cash.turbine.test
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.network.model.FriendData
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import com.kumpello.whereiseveryone.data.network.model.UserInfo
import com.kumpello.whereiseveryone.feature.main.location.LocationService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Test

class ProximityManagerTest {

    private val locationService: LocationService = mockk()
    private val friendsManager: FriendsStateRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    @Test
    fun `observeNearbyFriends filters friends based on threshold`() = runTest {
        val proximityManager = ProximityManager(locationService, friendsManager, preferencesManager, StandardTestDispatcher(testScheduler))
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
        coEvery { friendsManager.observeFriends() } returns flowOf(friendsResponse)
        every { preferencesManager.observe(PreferencesKey.ProximityDistance) } returns flowOf(1000) // 1km threshold

        proximityManager.observeNearbyFriends().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("near", result[0])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `proximity reacts to successive fixes and location loss without waiting`() = runTest {
        val locations = MutableStateFlow<Location?>(null)
        val near = gpsFix(0.0)
        val far = gpsFix(1.0)
        every { locationService.observeLocation() } returns locations
        coEvery { friendsManager.observeFriends() } returns flowOf(FriendsResponse.FriendsData(listOf(
            FriendData("near", "", "accepted", UserInfo(0.0, 0.0, 0f, 0.0, 0f, 0f, 0L), 0L)
        )))
        every { preferencesManager.observe(PreferencesKey.ProximityDistance) } returns flowOf(1000)
        val manager = ProximityManager(locationService, friendsManager, preferencesManager, StandardTestDispatcher(testScheduler))

        manager.observeNearbyFriends().test {
            runCurrent()
            assertEquals(emptyList<String>(), expectMostRecentItem())
            locations.value = near
            runCurrent()
            assertEquals(listOf("near"), expectMostRecentItem())
            locations.value = far
            runCurrent()
            assertEquals(emptyList<String>(), expectMostRecentItem())
            locations.value = near
            runCurrent()
            assertEquals(listOf("near"), expectMostRecentItem())
            locations.value = null
            runCurrent()
            assertEquals(emptyList<String>(), expectMostRecentItem())
            assertEquals(0L, testScheduler.currentTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun gpsFix(lat: Double) = mockk<Location> {
        every { latitude } returns lat
        every { longitude } returns 0.0
        every { altitude } returns 0.0
    }
}
