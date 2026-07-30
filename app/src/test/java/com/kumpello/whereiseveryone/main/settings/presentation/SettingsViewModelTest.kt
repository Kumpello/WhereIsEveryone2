package com.kumpello.whereiseveryone.main.settings.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.usecase.LogoutUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locationService: LocationService = mockk(relaxed = true) {
        every { observeIsServiceRunning() } returns MutableStateFlow(true).asStateFlow()
    }
    private val wipeLocationUseCase: WipeLocationUseCase = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true) {
        every { observe(PreferencesKey.ProximityDistance) } returns emptyFlow()
    }
    private val logoutUseCase: LogoutUseCase = mockk(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    private fun setupViewModel() {
        viewModel = SettingsViewModel(
            locationService,
            wipeLocationUseCase,
            preferencesManager,
            logoutUseCase
        )
    }

    @Test
    fun `SwitchLocationServiceState when running stops service and updates preferences`() = runTest {
        setupViewModel()

        viewModel.trigger(SettingsViewModel.Event.SwitchLocationServiceState)

        coVerify {
            locationService.stopLocationService()
            preferencesManager.save(PreferencesKey.LocationSharingEnabled, false)
        }
    }

    @Test
    fun `ClearData success stops service and saves preference`() = runTest {
        coEvery { wipeLocationUseCase.execute() } returns CodeResponse.SuccessNoContent
        setupViewModel()

        viewModel.trigger(SettingsViewModel.Event.ClearData)

        coVerify {
            locationService.stopLocationService()
            preferencesManager.save(PreferencesKey.LocationSharingEnabled, false)
        }
    }

    @Test
    fun `Logout stops service, executes logout, and triggers NavigateToAuth action`() = runTest {
        setupViewModel()

        viewModel.action.test {
            viewModel.trigger(SettingsViewModel.Event.Logout)
            assertEquals(SettingsViewModel.Action.NavigateToAuth, awaitItem())
        }

        coVerify {
            locationService.stopLocationService()
            logoutUseCase.execute()
        }
    }

    @Test
    fun `ChangeProximityDistance updates state`() = runTest {
        setupViewModel()

        viewModel.state.test {
            awaitItem() // Initial state
            viewModel.trigger(SettingsViewModel.Event.ChangeProximityDistance(100))
            assertEquals(100, awaitItem().proximityDistance)
        }
    }
}
