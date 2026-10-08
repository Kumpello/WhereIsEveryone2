package com.kumpello.whereiseveryone.feature.main.ui.settings

import app.cash.turbine.test
import com.kumpello.whereiseveryone.feature.main.R
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.session.LogoutUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.feature.main.location.LocationService
import com.kumpello.whereiseveryone.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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

    private val serviceRunning = MutableStateFlow(true)
    private val locationService: LocationService = mockk(relaxed = true) {
        every { observeIsServiceRunning() } returns serviceRunning.asStateFlow()
    }
    private val wipeLocationUseCase: WipeLocationUseCase = mockk()
    private val sharingEnabled = MutableStateFlow<Boolean?>(true)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true) {
        every { observe(PreferencesKey.ProximityDistance) } returns emptyFlow()
        every { observe(PreferencesKey.LocationSharingEnabled) } returns sharingEnabled
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
    fun `notification pause shows sharing stopped while service remains alive`() = runTest {
        setupViewModel()
        viewModel.state.test {
            if (!awaitItem().isLocationServiceRunning) assertTrue(awaitItem().isLocationServiceRunning)
            sharingEnabled.value = false
            val paused = awaitItem()
            assertTrue(paused.isLocationServiceRunning)
            assertFalse(paused.isSharingEnabled)
            assertEquals(R.string.settings_stop_location_service, paused.locationSwitchTextId)
            assertEquals(R.string.settings_start_sharing_location, paused.sharingSwitchTextId)
            sharingEnabled.value = true
            val resumed = awaitItem()
            assertTrue(resumed.isLocationServiceRunning)
            assertTrue(resumed.isSharingEnabled)
            assertEquals(R.string.settings_stop_sharing_location, resumed.sharingSwitchTextId)
        }
    }

    @Test
    fun `resuming from settings saves enabled before starting service`() = runTest {
        sharingEnabled.value = false
        serviceRunning.value = false
        setupViewModel()
        viewModel.trigger(SettingsViewModel.Event.SwitchLocationServiceState)
        coVerifyOrder {
            preferencesManager.save(PreferencesKey.LocationSharingEnabled, true)
            locationService.startLocationService()
        }
    }

    @Test
    fun `settings toggles sharing through service without stopping or restarting it`() = runTest {
        setupViewModel()
        viewModel.trigger(SettingsViewModel.Event.ToggleSharing)
        sharingEnabled.value = false
        viewModel.trigger(SettingsViewModel.Event.ToggleSharing)

        coVerify(exactly = 2) { locationService.toggleSharing() }
        coVerify(exactly = 0) { locationService.stopLocationService() }
        coVerify(exactly = 0) { locationService.startLocationService() }
        coVerify(exactly = 0) { preferencesManager.save(PreferencesKey.LocationSharingEnabled, any()) }
    }

    @Test
    fun `stopped service hides sharing control and ignores stale toggle clicks`() = runTest {
        serviceRunning.value = false
        setupViewModel()
        viewModel.state.test {
            val stopped = awaitItem()
            assertFalse(stopped.isLocationServiceRunning)
            assertFalse(stopped.isSharingEnabled)
            assertEquals(R.string.settings_start_location_service, stopped.locationSwitchTextId)
        }
        viewModel.trigger(SettingsViewModel.Event.ToggleSharing)
        coVerify(exactly = 0) { locationService.toggleSharing() }
    }

    @Test
    fun `ClearData stops sharing before deleting location`() = runTest {
        coEvery { wipeLocationUseCase.execute() } returns CodeResponse.SuccessNoContent
        setupViewModel()

        viewModel.trigger(SettingsViewModel.Event.ClearData)

        coVerifyOrder {
            locationService.stopLocationService()
            preferencesManager.save(PreferencesKey.LocationSharingEnabled, false)
            wipeLocationUseCase.execute()
        }
    }

    @Test
    fun `clear waits for an earlier resume before stopping uploads`() = runTest {
        sharingEnabled.value = false
        serviceRunning.value = false
        val resumeSaved = CompletableDeferred<Unit>()
        coEvery { preferencesManager.save(PreferencesKey.LocationSharingEnabled, true) } coAnswers {
            resumeSaved.await()
        }
        coEvery { wipeLocationUseCase.execute() } returns CodeResponse.SuccessNoContent
        setupViewModel()

        viewModel.action.test {
            viewModel.trigger(SettingsViewModel.Event.SwitchLocationServiceState)
            viewModel.trigger(SettingsViewModel.Event.ClearData)
            coVerify(exactly = 0) { wipeLocationUseCase.execute() }
            resumeSaved.complete(Unit)
            assertEquals(SettingsViewModel.Action.Toast(R.string.location_wiped_correctly_sharing_stoped), awaitItem())
        }
        coVerifyOrder {
            locationService.startLocationService()
            locationService.stopLocationService()
            preferencesManager.save(PreferencesKey.LocationSharingEnabled, false)
            wipeLocationUseCase.execute()
        }
    }

    @Test
    fun `failed deletion leaves sharing stopped`() = runTest {
        coEvery { wipeLocationUseCase.execute() } returns CodeResponse.ErrorData(500, "error", "error")
        setupViewModel()
        viewModel.action.test {
            viewModel.trigger(SettingsViewModel.Event.ClearData)
            assertEquals(SettingsViewModel.Action.Toast(R.string.error_wiping_location), awaitItem())
        }
        coVerifyOrder {
            locationService.stopLocationService()
            preferencesManager.save(PreferencesKey.LocationSharingEnabled, false)
            wipeLocationUseCase.execute()
        }
        coVerify(exactly = 0) { locationService.startLocationService() }
    }

    @Test
    fun `failed preference save prevents deletion`() = runTest {
        coEvery { preferencesManager.save(PreferencesKey.LocationSharingEnabled, false) } throws IOException("disk failure")
        setupViewModel()
        viewModel.action.test {
            viewModel.trigger(SettingsViewModel.Event.ClearData)
            assertEquals(SettingsViewModel.Action.Toast(R.string.error_wiping_location), awaitItem())
        }
        coVerify { locationService.stopLocationService() }
        coVerify(exactly = 0) { wipeLocationUseCase.execute() }
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
            if (!awaitItem().isLocationServiceRunning) awaitItem()
            viewModel.trigger(SettingsViewModel.Event.ChangeProximityDistance(100))
            assertEquals(100, awaitItem().proximityDistance)
        }
    }
}
