package com.kumpello.whereiseveryone.main.friends.presentation

import android.location.Location
import app.cash.turbine.test
import androidx.lifecycle.ViewModelStore
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filter
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
        coEvery { getPausedFriendsUseCase.execute() } returns SharingResponse.PausedFriends(emptyList())
        createViewModel()
    }

    private fun createViewModel() {
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
            preferencesManager,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    @Test
    fun `slow username load does not delay location observation or friends loading`() = runTest {
        val username = CompletableDeferred<String>()
        val locations = MutableStateFlow<Location?>(null)
        coEvery { preferencesManager.get(PreferencesKey.UserName) } coAnswers { username.await() }
        coEvery { getFriendsDataUseCase.execute() } returns FriendsResponse.FriendsData(emptyList())
        coEvery { getPausedFriendsUseCase.execute() } returns SharingResponse.PausedFriends(emptyList())
        every { locationService.observeLocation() } returns locations
        createViewModel()
        val store = ViewModelStore().apply { put("friends", viewModel) }
        try {
            assertEquals(1, locations.subscriptionCount.value)
            coVerify(exactly = 1) { getFriendsDataUseCase.execute() }
            username.complete("alice")
            viewModel.state.filter { it.username.isNotEmpty() }.test {
                assertEquals("alice", awaitItem().username)
            }
        } finally {
            store.clear()
        }
    }

    @Test
    fun `initial state triggers CheckFriends`() = runTest {
        setupViewModel()

        viewModel.state.filter { it.username.isNotEmpty() }.test {
            val initialState = awaitItem()
            assertTrue(initialState.friends.isEmpty())
        }
    }

    @Test
    fun `OpenNfcSharingDialog event updates state and triggers action`() = runTest {
        setupViewModel("testuser")
        
        viewModel.action.test {
            viewModel.state.filter { it.username.isNotEmpty() }.test {
                awaitItem() // Initial
                viewModel.trigger(FriendsViewModel.Event.OpenNfcSharingDialog)
                val state = awaitItem()
                assertTrue(state.isNfcSharingDialogOpen)
                assertEquals("testuser", state.username)
            }
            val action = awaitItem()
            assertTrue(action is FriendsViewModel.Action.TriggerNfcSharing)
            assertEquals("testuser", (action as FriendsViewModel.Action.TriggerNfcSharing).username)
        }
    }

    @Test
    fun `CloseNfcSharingDialog event updates state and triggers StopNfcSharing action`() = runTest {
        setupViewModel("testuser")
        
        viewModel.action.test {
            viewModel.trigger(FriendsViewModel.Event.OpenNfcSharingDialog)

            viewModel.state.filter { it.username.isNotEmpty() }.test {
                assertTrue(awaitItem().isNfcSharingDialogOpen)
                viewModel.trigger(FriendsViewModel.Event.CloseNfcSharingDialog)
                assertFalse(awaitItem().isNfcSharingDialogOpen)
            }

            assertTrue(awaitItem() is FriendsViewModel.Action.TriggerNfcSharing)
            assertTrue(awaitItem() is FriendsViewModel.Action.StopNfcSharing)
        }
    }
}
