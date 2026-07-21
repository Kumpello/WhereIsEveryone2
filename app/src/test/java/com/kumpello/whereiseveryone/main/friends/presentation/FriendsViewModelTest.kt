package com.kumpello.whereiseveryone.main.friends.presentation

import android.location.Location
import app.cash.turbine.test
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AcceptFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.GetPausedFriendsUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RejectFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.ResumeSharingUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.StopSharingUseCase
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
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

class FriendsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val removeFriendUseCase: RemoveFriendUseCase = mockk()
    private val getFriendsDataUseCase: GetFriendsDataUseCase = mockk()
    private val acceptFriendUseCase: AcceptFriendUseCase = mockk()
    private val rejectFriendUseCase: RejectFriendUseCase = mockk()
    private val locationService: LocationService = mockk {
        every { observeLocation() } returns MutableStateFlow<Location?>(null).asStateFlow()
    }
    private val mapFriendUseCase: MapFriendUseCase = mockk()
    private val stopSharingUseCase: StopSharingUseCase = mockk()
    private val resumeSharingUseCase: ResumeSharingUseCase = mockk()
    private val getPausedFriendsUseCase: GetPausedFriendsUseCase = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    private lateinit var viewModel: FriendsViewModel

    private fun setupViewModel(username: String = "testuser") {
        coEvery { preferencesManager.get(PreferencesKey.UserName) } returns username
        coEvery { getFriendsDataUseCase.execute() } returns FriendsResponse.FriendsData(emptyList())
        coEvery { getPausedFriendsUseCase.execute() } returns mockk()
        
        viewModel = FriendsViewModel(
            removeFriendUseCase,
            getFriendsDataUseCase,
            acceptFriendUseCase,
            rejectFriendUseCase,
            locationService,
            mapFriendUseCase,
            stopSharingUseCase,
            resumeSharingUseCase,
            getPausedFriendsUseCase,
            preferencesManager
        )
    }

    @Test
    fun `initial state triggers CheckFriends`() = runTest {
        setupViewModel()

        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue(initialState.friends.isEmpty())
        }
    }

    @Test
    fun `OpenNfcSharingDialog event updates state and triggers action`() = runTest {
        setupViewModel("testuser")
        viewModel.state.test {
            awaitItem() // Initial
            viewModel.trigger(FriendsViewModel.Event.OpenNfcSharingDialog)
            val state = awaitItem()
            assertTrue(state.isNfcSharingDialogOpen)
            assertEquals("testuser", state.username)
        }

        viewModel.action.test {
            val action = awaitItem()
            assertTrue(action is FriendsViewModel.Action.TriggerNfcSharing)
            assertEquals("testuser", (action as FriendsViewModel.Action.TriggerNfcSharing).username)
        }
    }

    @Test
    fun `CloseNfcSharingDialog event updates state and triggers StopNfcSharing action`() = runTest {
        setupViewModel("testuser")
        
        viewModel.trigger(FriendsViewModel.Event.OpenNfcSharingDialog)

        viewModel.state.test {
            assertTrue(awaitItem().isNfcSharingDialogOpen)
            viewModel.trigger(FriendsViewModel.Event.CloseNfcSharingDialog)
            assertFalse(awaitItem().isNfcSharingDialogOpen)
        }

        viewModel.action.test {
            assertTrue(awaitItem() is FriendsViewModel.Action.TriggerNfcSharing)
            assertTrue(awaitItem() is FriendsViewModel.Action.StopNfcSharing)
        }
    }
}
