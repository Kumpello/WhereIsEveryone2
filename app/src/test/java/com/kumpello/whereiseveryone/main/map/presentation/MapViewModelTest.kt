package com.kumpello.whereiseveryone.main.map.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.main.common.domain.manager.FriendsManager
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapFriendUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapLocationUseCase
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.LocationData
import com.kumpello.whereiseveryone.main.friends.domain.usecase.GetPausedFriendsUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.ResumeSharingUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.StopSharingUseCase
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import android.location.Location as AndroidLocation
import com.kumpello.whereiseveryone.main.common.entity.Location as EntityLocation

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locationService: LocationService = mockk {
        every { observeLocation() } returns MutableStateFlow<AndroidLocation?>(null).asStateFlow()
    }
    private val friendsManager: FriendsManager = mockk {
        every { observeFriends() } returns emptyFlow()
    }
    private val mapLocationUseCase: MapLocationUseCase = mockk()
    private val mapFriendUseCase: MapFriendUseCase = mockk()
    private val stopSharingUseCase: StopSharingUseCase = mockk()
    private val resumeSharingUseCase: ResumeSharingUseCase = mockk()
    private val getPausedFriendsUseCase: GetPausedFriendsUseCase = mockk()

    private lateinit var viewModel: MapViewModel

    private fun setupViewModel() {
        viewModel = MapViewModel(
            locationService,
            friendsManager,
            mapLocationUseCase,
            mapFriendUseCase,
            stopSharingUseCase,
            resumeSharingUseCase,
            getPausedFriendsUseCase,
        )
    }

    @Test
    fun `initial state has no user or friends`() = runTest {
        setupViewModel()
        viewModel.state.test {
            val initialState = awaitItem()
            assertNull(initialState.user)
            assertEquals(0, initialState.friends.size)
        }
    }

    @Test
    fun `ZoomIn updates map settings`() = runTest {
        setupViewModel()
        viewModel.state.test {
            val initial = awaitItem()
            val initialZoom = initial.mapSettings.zoom
            viewModel.trigger(MapViewModel.Event.ZoomIn)
            assertEquals(initialZoom + 0.5, awaitItem().mapSettings.zoom, 0.001)
        }
    }

    @Test
    fun `ZoomOut updates map settings`() = runTest {
        setupViewModel()
        viewModel.state.test {
            val initial = awaitItem()
            val initialZoom = initial.mapSettings.zoom
            viewModel.trigger(MapViewModel.Event.ZoomOut)
            assertEquals(initialZoom - 0.5, awaitItem().mapSettings.zoom, 0.001)
        }
    }

    @Test
    fun `OnCameraUpdate updates map bearing`() = runTest {
        setupViewModel()
        viewModel.state.test {
            awaitItem() // initial
            viewModel.trigger(MapViewModel.Event.OnCameraUpdate(45.0))
            assertEquals(45.0, awaitItem().mapSettings.bearing, 0.001)
        }
    }

    @Test
    fun `bearingToFriend calculation accounts for map bearing`() = runTest {
        val userLocation = LocationData(lat = 0.0, lon = 0.0, bearing = 0f, alt = 0.0, accuracy = 0f, lastUpdate = 0L)
        val entityLocation = EntityLocation(
            lat = 1.0, lon = 0.0, bearing = 0f,
            alt = AltDifference.SOMEWHAT_SAME, rawAlt = 0.0,
            accuracy = AccuracyLevel.PERFECT, rawAccuracy = 0f,
            lastUpdateTime = "now", lastUpdateAge = LastUpdateAge.FRESH
        )
        val friend = Friend(username = "friend", status = "", state = FriendState.ACCEPTED, location = entityLocation) // North

        every { mapLocationUseCase.execute(any()) } returns EntityLocation(
            lat = 0.0, lon = 0.0, bearing = 0f,
            alt = AltDifference.SOMEWHAT_SAME, rawAlt = 0.0,
            accuracy = AccuracyLevel.PERFECT, rawAccuracy = 0f,
            lastUpdateTime = "now", lastUpdateAge = LastUpdateAge.FRESH
        )
        every { mapFriendUseCase.execute(any(), any()) } returns friend

        setupViewModel()
        viewModel.trigger(MapViewModel.Event.OnLocationUpdate(userLocation))
        viewModel.trigger(MapViewModel.Event.NavigateToFriend(friend))

        viewModel.state.test {
            val initial = awaitItem()
            
            // Case 1: Map pointing North (bearing 0)
            assertEquals(0.0f, initial.bearingToFriend!!, 0.001f)

            // Case 2: Map pointing East (bearing 90)
            viewModel.trigger(MapViewModel.Event.OnCameraUpdate(90.0))
            val updated = awaitItem()
            // Friend is North (0 deg). Map is East (90 deg). 
            // Relative bearing = (0 - 90 + 360) % 360 = 270
            assertEquals(270.0f, updated.bearingToFriend!!, 0.001f)
        }
    }
}
