package com.kumpello.whereiseveryone.main.map.presentation

import android.location.Location
import app.cash.turbine.test
import com.kumpello.whereiseveryone.main.common.domain.manager.FriendsManager
import com.kumpello.whereiseveryone.main.common.domain.usecase.CalculateBearingUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapFriendUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapLocationUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.StopSharingUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.ResumeSharingUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.GetPausedFriendsUseCase
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

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locationService: LocationService = mockk {
        every { observeLocation() } returns MutableStateFlow<Location?>(null).asStateFlow()
    }
    private val friendsManager: FriendsManager = mockk {
        every { observeFriends() } returns emptyFlow()
    }
    private val mapLocationUseCase: MapLocationUseCase = mockk()
    private val mapFriendUseCase: MapFriendUseCase = mockk()
    private val calculateBearingUseCase: CalculateBearingUseCase = mockk()
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
            calculateBearingUseCase,
            stopSharingUseCase,
            resumeSharingUseCase,
            getPausedFriendsUseCase
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
}
