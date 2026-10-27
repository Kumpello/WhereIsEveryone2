package com.kumpello.whereiseveryone.feature.main.ui.map

import android.location.Location
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.kumpello.whereiseveryone.core.testing.MainDispatcherRule
import com.kumpello.whereiseveryone.data.model.FriendLocalData
import com.kumpello.whereiseveryone.data.model.FriendState
import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.model.PointOfInterest
import com.kumpello.whereiseveryone.data.network.model.PointOfInterestRequest
import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.data.repository.PointsOfInterestRepository
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.feature.main.R
import com.kumpello.whereiseveryone.feature.main.location.LocationService
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapFriendUseCase
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapLocationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PointsOfInterestViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    private val repository = mockk<PointsOfInterestRepository>()

    @After
    fun clearViewModels() = store.clear()

    @Test
    fun `creating a point sends saved coordinates and trimmed text only once while saving`() = runTest {
        val result = CompletableDeferred<PointOfInterest>()
        val point = point("one")
        coEvery { repository.create(any()) } coAnswers { result.await() }
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.trigger(MapViewModel.Event.OnOwnPointsLoaded(emptyList()))
            vm.trigger(MapViewModel.Event.OnMapLongClick(point.latitude, point.longitude))
            vm.trigger(MapViewModel.Event.ChangePointName("  Bridge  "))
            vm.trigger(MapViewModel.Event.ChangePointDescription("  Meet here  "))
            vm.trigger(MapViewModel.Event.SavePoint)
            vm.trigger(MapViewModel.Event.SavePoint)
            runCurrent()
            assertTrue(expectMostRecentItem().pointsOfInterest.isSaving)
            coVerify(exactly = 1) { repository.create(PointOfInterestRequest("Bridge", "Meet here", point.latitude, point.longitude)) }
            result.complete(point)
            runCurrent()
            val saved = expectMostRecentItem()
            assertEquals(listOf(point), saved.points)
            assertNull(saved.pointsOfInterest.draft)
            assertFalse(saved.pointsOfInterest.isSaving)
            assertTrue(saved.pointsOfInterest.showLayer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `five points block creation and deleting one frees a slot`() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.trigger(MapViewModel.Event.OnOwnPointsLoaded((1..5).map { point(it.toString()) }))
            vm.trigger(MapViewModel.Event.OnMapLongClick(52.0, 21.0))
            runCurrent()
            val full = expectMostRecentItem().pointsOfInterest
            assertNull(full.draft)
            assertTrue(full.showManager)
            assertEquals(R.string.poi_limit_reached, full.error)
            vm.trigger(MapViewModel.Event.OnPointDeleted("1"))
            vm.trigger(MapViewModel.Event.OnMapLongClick(52.0, 21.0))
            runCurrent()
            assertNotNull(expectMostRecentItem().pointsOfInterest.draft)
            coVerify(exactly = 0) { repository.create(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed saves preserve the draft for retry`() = runTest {
        coEvery { repository.create(any()) } throws IOException("Offline")
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.trigger(MapViewModel.Event.OnOwnPointsLoaded(emptyList()))
            vm.trigger(MapViewModel.Event.OnMapLongClick(52.0, 21.0))
            vm.trigger(MapViewModel.Event.ChangePointName("Bridge"))
            vm.trigger(MapViewModel.Event.SavePoint)
            runCurrent()
            val failed = expectMostRecentItem().pointsOfInterest
            assertEquals("Bridge", failed.draft?.name)
            assertEquals(R.string.poi_save_error, failed.error)
            assertFalse(failed.isSaving)
            coEvery { repository.create(any()) } returns point("saved")
            vm.trigger(MapViewModel.Event.SavePoint)
            runCurrent()
            val retried = expectMostRecentItem().pointsOfInterest
            assertNull(retried.draft)
            assertEquals("saved", retried.ownPoints.single().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `friend points stay visible with paused location and navigation uses their fixed coordinates`() = runTest {
        val vm = viewModel()
        val destination = point("friend").copy(latitude = 0.0, longitude = 0.001, isOwn = false, creatorUsername = "bob")
        val friend = FriendLocalData("bob", "", FriendState.ACCEPTED, null, null, listOf(destination))
        vm.state.test {
            awaitItem()
            vm.trigger(MapViewModel.Event.OnFriendsLoaded(listOf(friend)))
            vm.trigger(MapViewModel.Event.OnPausedFriendsLoaded(listOf("bob")))
            vm.trigger(MapViewModel.Event.OnLocationUpdate(LocationData(0.0, 0.0, null, null, null, null, 1_800_000_000_000)))
            vm.trigger(MapViewModel.Event.OnPointClick(destination.id))
            runCurrent()
            assertEquals(destination, expectMostRecentItem().selectedPoint)
            vm.trigger(MapViewModel.Event.NavigateToPoint(destination.id))
            vm.trigger(MapViewModel.Event.OnCameraUpdate(90.0))
            runCurrent()
            val navigation = expectMostRecentItem()
            assertEquals(destination, navigation.navigatingPoint)
            assertEquals(0f, requireNotNull(navigation.bearingToPoint), 0.001f)
            assertEquals("111 m", navigation.distanceToPoint)
            assertNull(navigation.navigatingFriend)
            vm.trigger(MapViewModel.Event.OnFriendsLoaded(emptyList()))
            runCurrent()
            val removed = expectMostRecentItem()
            assertTrue(removed.points.isEmpty())
            assertNull(removed.navigatingPoint)
            assertNull(removed.bearingToPoint)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `friends points cannot be edited or deleted`() = runTest {
        val vm = viewModel()
        val shared = point("shared").copy(isOwn = false, creatorUsername = "bob")
        vm.state.test {
            awaitItem()
            vm.trigger(MapViewModel.Event.OnFriendsLoaded(listOf(FriendLocalData("bob", "", FriendState.ACCEPTED, null, null, listOf(shared)))))
            vm.trigger(MapViewModel.Event.OnPointClick(shared.id))
            vm.trigger(MapViewModel.Event.EditSelectedPoint)
            vm.trigger(MapViewModel.Event.RequestPointDeletion)
            vm.trigger(MapViewModel.Event.DeletePoint)
            runCurrent()
            val state = expectMostRecentItem().pointsOfInterest
            assertNull(state.draft)
            assertNull(state.deleteConfirmationId)
            coVerify(exactly = 0) { repository.update(any(), any()) }
            coVerify(exactly = 0) { repository.delete(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed deletion retains the point and successful retry clears its navigation`() = runTest {
        val vm = viewModel()
        val own = point("own")
        coEvery { repository.delete(own.id) } throws IOException("Offline")
        vm.state.test {
            awaitItem()
            vm.trigger(MapViewModel.Event.OnOwnPointsLoaded(listOf(own)))
            vm.trigger(MapViewModel.Event.NavigateToPoint(own.id))
            vm.trigger(MapViewModel.Event.OnPointClick(own.id))
            vm.trigger(MapViewModel.Event.RequestPointDeletion)
            vm.trigger(MapViewModel.Event.DeletePoint)
            runCurrent()
            val failed = expectMostRecentItem()
            assertEquals(listOf(own), failed.points)
            assertEquals(R.string.poi_delete_error, failed.pointsOfInterest.error)
            coEvery { repository.delete(own.id) } returns Unit
            vm.trigger(MapViewModel.Event.DeletePoint)
            runCurrent()
            val deleted = expectMostRecentItem()
            assertTrue(deleted.points.isEmpty())
            assertNull(deleted.navigatingPoint)
            assertNull(deleted.pointsOfInterest.deleteConfirmationId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mutations wait for an in flight refresh so an old list cannot restore a deleted point`() = runTest {
        val own = point("own")
        val loaded = CompletableDeferred<List<PointOfInterest>>()
        coEvery { repository.getOwnPoints() } coAnswers { loaded.await() }
        coEvery { repository.delete(own.id) } returns Unit
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.trigger(MapViewModel.Event.OnOwnPointsLoaded(listOf(own)))
            vm.trigger(MapViewModel.Event.LoadOwnPoints)
            vm.trigger(MapViewModel.Event.OnPointClick(own.id))
            vm.trigger(MapViewModel.Event.RequestPointDeletion)
            vm.trigger(MapViewModel.Event.DeletePoint)
            runCurrent()
            assertTrue(expectMostRecentItem().pointsOfInterest.isLoading)
            coVerify(exactly = 0) { repository.delete(any()) }
            loaded.complete(listOf(own))
            runCurrent()
            vm.trigger(MapViewModel.Event.DeletePoint)
            runCurrent()
            assertTrue(expectMostRecentItem().points.isEmpty())
            coVerify(exactly = 1) { repository.delete(own.id) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(): MapViewModel = MapViewModel(
        locationService = mockk<LocationService> { every { observeLocation() } returns MutableStateFlow<Location?>(null) },
        friendsManager = mockk<FriendsStateRepository> { coEvery { observeFriends() } returns emptyFlow() },
        mapLocationUseCase = MapLocationUseCase(), mapFriendUseCase = MapFriendUseCase(),
        stopSharingUseCase = mockk(), resumeSharingUseCase = mockk(), getPausedFriendsUseCase = mockk(),
        preferencesManager = mockk<PreferencesManager> {
            every { observe(PreferencesKey.CarMode) } returns emptyFlow()
            every { observe(PreferencesKey.MapFpsLimit) } returns emptyFlow()
        },
        roadNavigationRepository = mockk(relaxed = true), pointsOfInterestRepository = repository,
        defaultDispatcher = dispatcherRule.testDispatcher,
    ).also { store.put("map", it) }

    private fun point(id: String) = PointOfInterest(id, "Bridge", "Meet here", 52.0, 21.0, "alice", 1_800_000_000_000, true)
}
