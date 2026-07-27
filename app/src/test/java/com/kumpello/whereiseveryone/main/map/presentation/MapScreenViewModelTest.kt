package com.kumpello.whereiseveryone.main.map.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetPermissionsStatusUseCase
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
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

    private fun setupViewModel() {
        viewModel = MapScreenViewModel(getPermissionsStatusUseCase, locationService)
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
}
