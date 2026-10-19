package com.kumpello.whereiseveryone.feature.main.ui.map

import com.kumpello.whereiseveryone.feature.main.location.LocationService
import app.cash.turbine.test
import androidx.lifecycle.ViewModelStore
import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.core.presentation.ScreenState
import com.kumpello.whereiseveryone.feature.main.ui.permissions.GetPermissionsStatusUseCase
import com.kumpello.whereiseveryone.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPermissionsStatusUseCase: GetPermissionsStatusUseCase = mockk()
    private val locationService: LocationService = mockk {
        every { observeForcedForegroundStatus() } returns MutableStateFlow(
            LocationService.ForcedForegroundStatus(false, null)
        ).asStateFlow()
    }
    private lateinit var viewModel: MapScreenViewModel
    private val incomingRequests = MutableStateFlow<List<String>>(emptyList())
    private val friendsStateRepository: FriendsStateRepository = mockk {
        every { incomingFriendRequests } returns incomingRequests.asStateFlow()
    }

    @Test
    fun `friends badge follows incoming request count and disappears after last request`() = runTest {
        setupViewModel()
        val store = ViewModelStore().apply { put("mapScreen", viewModel) }
        try {
            viewModel.state.test {
                assertEquals(0, awaitItem().incomingFriendRequestCount)
                incomingRequests.value = listOf("alice", "bob")
                assertEquals(2, awaitItem().incomingFriendRequestCount)
                incomingRequests.value = listOf("bob")
                assertEquals(1, awaitItem().incomingFriendRequestCount)
                incomingRequests.value = emptyList()
                assertEquals(0, awaitItem().incomingFriendRequestCount)
            }
        } finally {
            store.clear()
        }
    }

    @Test
    fun `notification navigation remains pending until friends screen opens`() = runTest {
        setupViewModel()
        viewModel.trigger(MapScreenViewModel.Event.OpenIncomingFriendRequests)
        viewModel.state.test {
            assertTrue(awaitItem().openIncomingFriendRequests)
            viewModel.trigger(MapScreenViewModel.Event.IncomingFriendRequestsOpened)
            assertFalse(awaitItem().openIncomingFriendRequests)
        }
    }

    @Test
    fun `NavigateMessage updates screenState`() = runTest {
        setupViewModel()
        viewModel.state.test {
            assertEquals(ScreenState.Map, awaitItem().screenState) // Initial
            viewModel.trigger(MapScreenViewModel.Event.NavigateMessage)
            assertEquals(ScreenState.Message, awaitItem().screenState)
        }
    }

    @Test
    fun `BackToMap updates screenState`() = runTest {
        setupViewModel()
        viewModel.state.test {
            awaitItem() // Initial
            viewModel.trigger(MapScreenViewModel.Event.NavigateMessage)
            awaitItem()
            viewModel.trigger(MapScreenViewModel.Event.BackToMap)
            assertEquals(ScreenState.Map, awaitItem().screenState)
        }
    }

    @Test
    fun `showPermissionNotification is true when permissions are missing`() = runTest {
        setupViewModel()
        viewModel.state.test {
            awaitItem() // Initial (empty map, containsValue(false) is false)
            viewModel.trigger(MapScreenViewModel.Event.SetPermissions(mapOf("p1" to false)))
            assertTrue(awaitItem().showPermissionNotification)
        }
    }

    @Test
    fun `OnPermissionDeny hides notification`() = runTest {
        setupViewModel()
        viewModel.state.test {
            awaitItem() // Initial
            viewModel.trigger(MapScreenViewModel.Event.SetPermissions(mapOf("p1" to false)))
            awaitItem()
            viewModel.trigger(MapScreenViewModel.Event.OnPermissionDeny)
            assertFalse(awaitItem().showPermissionNotification)
        }
    }

    @Test
    fun `NavigateFriends action is received by multiple collectors`() = runTest {
        setupViewModel()
        viewModel.action.test {
            val secondCollector = viewModel.action.testIn(this)
            
            viewModel.trigger(MapScreenViewModel.Event.OnPermissionAllow)
            
            val action1 = awaitItem()
            val action2 = secondCollector.awaitItem()
            
            assertTrue(action1 is MapScreenViewModel.Action.ShowPermissionSettings)
            assertTrue(action2 is MapScreenViewModel.Action.ShowPermissionSettings)
            assertEquals(action1, action2)
            
            secondCollector.cancel()
        }
    }

    private fun setupViewModel() {
        viewModel = MapScreenViewModel(getPermissionsStatusUseCase, locationService, friendsStateRepository)
    }
}
