package com.kumpello.whereiseveryone.feature.main.ui.map

import android.location.Location
import androidx.lifecycle.ViewModelStore
import com.kumpello.whereiseveryone.core.testing.MainDispatcherRule
import com.kumpello.whereiseveryone.data.model.FriendLocalData
import com.kumpello.whereiseveryone.data.model.FriendState
import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.data.repository.RoadNavigationRepository
import com.kumpello.whereiseveryone.data.repository.RoadTravelEstimate
import com.kumpello.whereiseveryone.data.repository.RoadInterceptionEstimate
import com.kumpello.whereiseveryone.data.repository.RoadPoint
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.feature.main.location.LocationService
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapFriendUseCase
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapLocationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CarModeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val epoch = 1_800_000_000_000L
    private val setting = MutableStateFlow<Boolean?>(false)
    private val preferences = mockk<PreferencesManager> {
        every { observe(PreferencesKey.CarMode) } returns setting
        every { observe(PreferencesKey.MapFpsLimit) } returns emptyFlow()
        coEvery { save(PreferencesKey.CarMode, any()) } answers { setting.value = secondArg() }
    }
    private val roads = mockk<RoadNavigationRepository> {
        coEvery { matchRoad(any()) } returns null
        coEvery { travelEstimate(any(), any()) } returns RoadTravelEstimate(360.0, 5000.0)
        coEvery { interceptionEstimate(any(), any(), any()) } returns RoadInterceptionEstimate.Intercept(600.0, 8_000.0, RoadPoint(52.01, 21.01))
    }
    private val store = ViewModelStore()
    @Test
    fun `speed suggestion requires consent and dismissal prevents repeated prompts`() = runTest {
        try {
            val vm = viewModel()
            vm.trigger(MapViewModel.Event.OnMapVisibilityChanged(true))
            vm.trigger(MapViewModel.Event.OnLocationUpdate(fix()))
            val suggestion = vm.state.first { it.showCarSuggestion }
            assertFalse(suggestion.isCarMode)
            coVerify(exactly = 0) { preferences.save(PreferencesKey.CarMode, any()) }
            vm.trigger(MapViewModel.Event.DismissCarSuggestion)
            vm.trigger(MapViewModel.Event.OnLocationUpdate(fix(20f)))
            assertFalse(vm.state.first { !it.showCarSuggestion }.isCarMode)
            vm.trigger(MapViewModel.Event.AcceptCarSuggestion)
            assertFalse(vm.state.value.isCarMode)
            coVerify(exactly = 0) { preferences.save(PreferencesKey.CarMode, any()) }
        } finally { store.clear() }
    }

    @Test
    fun `confirming a visible suggestion persists car mode`() = runTest {
        try {
            val vm = viewModel()
            vm.trigger(MapViewModel.Event.OnMapVisibilityChanged(true))
            vm.trigger(MapViewModel.Event.OnLocationUpdate(fix()))
            vm.state.first { it.showCarSuggestion }
            vm.trigger(MapViewModel.Event.AcceptCarSuggestion)
            assertTrue(vm.state.first { it.isCarMode }.user!!.isCarMode)
            coVerify(exactly = 1) { preferences.save(PreferencesKey.CarMode, true) }
        } finally { store.clear() }
    }

    @Test
    fun `tracking uses live driver speed and road travel duration and stops after leaving map`() = runTest {
        try {
            val vm = viewModel()
            val friend = FriendLocalData("driver", "", FriendState.ACCEPTED, fix(10f, true), null)
            vm.trigger(MapViewModel.Event.OnLocationUpdate(fix()))
            vm.trigger(MapViewModel.Event.OnFriendsLoaded(listOf(friend)))
            vm.trigger(MapViewModel.Event.OnMapVisibilityChanged(true))
            vm.trigger(MapViewModel.Event.NavigateToFriend(MapFriendUseCase().execute(friend, fix())))
            val tracking = requireNotNull(vm.state.first { it.carTrackingInfo?.travelSeconds != null }.carTrackingInfo)
            assertEquals(36.0, tracking.friendSpeedKmh!!, 0.0001)
            assertEquals(18.0, tracking.speedDifferenceKmh!!, 0.0001)
            assertEquals(360.0, tracking.travelSeconds!!, 0.0)
            vm.trigger(MapViewModel.Event.OnFriendsLoaded(listOf(friend.copy(location = fix(20f, true)))))
            val updated = vm.state.first { it.carTrackingInfo?.friendSpeedKmh == 72.0 }
            assertEquals(-18.0, updated.carTrackingInfo!!.speedDifferenceKmh!!, 0.0001)
            vm.trigger(MapViewModel.Event.OnMapVisibilityChanged(false))
            advanceTimeBy(60_000L)
            runCurrent()
            coVerify(exactly = 1) { roads.travelEstimate(any(), any()) }
            vm.trigger(MapViewModel.Event.CancelNavigation)
            vm.trigger(MapViewModel.Event.OnRoadUpdate("driver", null, null, RoadTravelEstimate(1.0, 1.0), epoch))
            assertNull(vm.state.first { it.navigatingFriend == null }.carTrackingInfo)
        } finally { store.clear() }
    }

    @Test
    fun `stopped GPS makes speed unknown instead of keeping a stale driving value`() = runTest {
        try {
            setting.value = true
            val vm = viewModel()
            vm.trigger(MapViewModel.Event.OnLocationUpdate(fix()))
            vm.trigger(MapViewModel.Event.OnMapVisibilityChanged(true))
            assertEquals(54.0, vm.state.first { it.speedKmh != null }.speedKmh!!, 0.0)
            advanceTimeBy(12_000L)
            runCurrent()
            assertNull(vm.state.first { it.speedKmh == null }.speedKmh)
        } finally { store.clear() }
    }

    @Test
    fun `both drivers use interception instead of routing to the last fix and stop offscreen`() = runTest {
        try {
            setting.value = true
            val vm = viewModel()
            vm.track(FriendLocalData("driver", "", FriendState.ACCEPTED, fix(10f, true), null))
            val info = vm.state.first { it.carTrackingInfo?.interceptionEstimate != null }.carTrackingInfo!!
            assertTrue(info.isPredictive)
            assertEquals(600.0, (info.interceptionEstimate as RoadInterceptionEstimate.Intercept).seconds, 0.0)
            assertNull(info.travelSeconds)
            coVerify(exactly = 1) { roads.interceptionEstimate(match { it.isCarMode }, match { it.isCarMode }, epoch) }
            coVerify(exactly = 0) { roads.travelEstimate(any(), any()) }
            vm.trigger(MapViewModel.Event.OnMapVisibilityChanged(false))
            advanceTimeBy(60_000)
            runCurrent()
            coVerify(exactly = 1) { roads.interceptionEstimate(any(), any(), any()) }
        } finally { store.clear() }
    }

    @Test
    fun `slower follower exposes arrival lateness and speed change discards old prediction`() = runTest {
        try {
            setting.value = true
            coEvery { roads.interceptionEstimate(any(), any(), any()) } returns RoadInterceptionEstimate.ArriveLater(400.0, 120.0, 6_000.0, RoadPoint(52.01, 21.01))
            val vm = viewModel()
            val driver = FriendLocalData("driver", "", FriendState.ACCEPTED, fix(20f, true), null)
            vm.track(driver)
            val info = vm.state.first { it.carTrackingInfo?.interceptionEstimate != null }.carTrackingInfo!!
            assertEquals(-18.0, info.speedDifferenceKmh!!, 0.001)
            assertEquals(120.0, (info.interceptionEstimate as RoadInterceptionEstimate.ArriveLater).secondsAfterDriver, 0.0)
            vm.trigger(MapViewModel.Event.OnFriendsLoaded(listOf(driver.copy(location = fix(30f, true)))))
            val changed = vm.state.first { it.carTrackingInfo?.friendSpeedKmh == 108.0 }.carTrackingInfo!!
            assertNull(changed.interceptionEstimate)
            coVerify(exactly = 1) { roads.interceptionEstimate(any(), any(), any()) }
        } finally { store.clear() }
    }

    @Test
    fun `prediction retains fifteen second pacing with fresh location updates`() = runTest {
        try {
            setting.value = true
            val vm = viewModel()
            val driver = FriendLocalData("driver", "", FriendState.ACCEPTED, fix(10f, true), null)
            vm.track(driver)
            vm.state.first { it.carTrackingInfo?.interceptionEstimate != null }
            advanceTimeBy(14_000)
            runCurrent()
            val time = epoch + mainDispatcherRule.testDispatcher.scheduler.currentTime
            vm.trigger(MapViewModel.Event.OnLocationUpdate(fix().copy(lastUpdate = time)))
            vm.trigger(MapViewModel.Event.OnFriendsLoaded(listOf(driver.copy(location = fix(10f, true).copy(lastUpdate = time)))))
            runCurrent()
            coVerify(exactly = 1) { roads.interceptionEstimate(any(), any(), any()) }
            advanceTimeBy(2_000)
            runCurrent()
            vm.state.first { it.carTrackingInfo?.interceptionEstimate != null }
            coVerify(exactly = 2) { roads.interceptionEstimate(any(), any(), any()) }
        } finally { store.clear() }
    }

    @Test
    fun `slow prediction is cancelled within the road refresh budget`() = runTest {
        try {
            setting.value = true
            var cancelled = false
            coEvery { roads.interceptionEstimate(any(), any(), any()) } coAnswers {
                try {
                    delay(20_000)
                    RoadInterceptionEstimate.Intercept(600.0, 8_000.0, RoadPoint(52.01, 21.01))
                } finally { cancelled = true }
            }
            val vm = viewModel()
            vm.track(FriendLocalData("driver", "", FriendState.ACCEPTED, fix(10f, true), null))
            runCurrent()
            advanceTimeBy(8_000)
            runCurrent()
            assertEquals(RoadInterceptionEstimate.Unavailable, vm.state.first { it.carTrackingInfo?.interceptionEstimate != null }.carTrackingInfo!!.interceptionEstimate)
            assertTrue(cancelled)
        } finally { store.clear() }
    }

    @Test
    fun `a queued result from a previous navigation or mode is rejected`() = runTest {
        try {
            setting.value = true
            val vm = viewModel()
            val driver = FriendLocalData("driver", "", FriendState.ACCEPTED, fix(10f, true), null)
            vm.track(driver)
            vm.state.first { it.carTrackingInfo?.interceptionEstimate != null }
            val prediction = CarInterceptionPrediction(RoadInterceptionEstimate.Intercept(60.0, 1_000.0, RoadPoint(52.01, 21.01)), fix(15f, true), fix(10f, true), epoch)
            vm.trigger(MapViewModel.Event.OnRoadUpdate("driver", null, null, null, epoch, version = 0, interception = prediction))
            runCurrent()
            val current = vm.state.first { it.carTrackingInfo?.interceptionEstimate != null }.carTrackingInfo!!.interceptionEstimate as RoadInterceptionEstimate.Intercept
            assertEquals(600.0, current.seconds, 0.0)
        } finally { store.clear() }
    }
    private fun viewModel(): MapViewModel = MapViewModel(
        locationService = mockk<LocationService> { every { observeLocation() } returns MutableStateFlow<Location?>(null) },
        friendsManager = mockk<FriendsStateRepository> { coEvery { observeFriends() } returns emptyFlow() },
        mapLocationUseCase = MapLocationUseCase(),
        mapFriendUseCase = MapFriendUseCase(),
        stopSharingUseCase = mockk(), resumeSharingUseCase = mockk(), getPausedFriendsUseCase = mockk(),
        preferencesManager = preferences, roadNavigationRepository = roads,
        pointsOfInterestRepository = mockk(relaxed = true),
        defaultDispatcher = mainDispatcherRule.testDispatcher,
        currentTimeMillis = { epoch + mainDispatcherRule.testDispatcher.scheduler.currentTime },
    ).also { store.put("map", it) }

    private fun fix(speed: Float = 15f, carMode: Boolean = false) =
        LocationData(52.0, 21.0, 90f, null, 10f, speed, epoch, carMode)

    private fun MapViewModel.track(friend: FriendLocalData) {
        trigger(MapViewModel.Event.OnLocationUpdate(fix()))
        trigger(MapViewModel.Event.OnFriendsLoaded(listOf(friend)))
        trigger(MapViewModel.Event.OnMapVisibilityChanged(true))
        trigger(MapViewModel.Event.NavigateToFriend(MapFriendUseCase().execute(friend, fix())))
    }


}
