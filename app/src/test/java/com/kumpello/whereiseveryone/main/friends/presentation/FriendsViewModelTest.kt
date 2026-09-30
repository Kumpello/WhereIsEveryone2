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
import com.kumpello.whereiseveryone.main.friends.nfc.NfcSharingSession
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val nfcSharingSession = NfcSharingSession { mainDispatcherRule.testDispatcher.scheduler.currentTime }

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
            nfcSharingSession,
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
                assertEquals("testuser", nfcSharingSession.current()?.username)
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
                assertEquals(null, nfcSharingSession.current())
            }

            assertTrue(awaitItem() is FriendsViewModel.Action.TriggerNfcSharing)
            assertTrue(awaitItem() is FriendsViewModel.Action.StopNfcSharing)
        }
    }

    @Test
    fun `sharing expires and closes the dialog without further NFC commands`() = runTest {
        setupViewModel()
        viewModel.action.test {
            viewModel.trigger(FriendsViewModel.Event.OpenNfcSharingDialog)
            assertTrue(awaitItem() is FriendsViewModel.Action.TriggerNfcSharing)
            advanceTimeBy(NfcSharingSession.TIMEOUT_MILLIS.milliseconds)
            runCurrent()
            assertTrue(awaitItem() is FriendsViewModel.Action.StopNfcSharing)
            assertEquals(null, nfcSharingSession.current())
            viewModel.state.filter { it.username.isNotEmpty() }.test {
                assertFalse(awaitItem().isNfcSharingDialogOpen)
            }
        }
    }

    @Test
    fun `repeated open events do not extend an active session`() = runTest {
        setupViewModel()
        viewModel.trigger(FriendsViewModel.Event.OpenNfcSharingDialog)
        advanceTimeBy((NfcSharingSession.TIMEOUT_MILLIS - 1).milliseconds)
        viewModel.trigger(FriendsViewModel.Event.OpenNfcSharingDialog)
        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(null, nfcSharingSession.current())
    }

    @Test
    fun `clearing the view model revokes sharing`() = runTest {
        setupViewModel()
        viewModel.trigger(FriendsViewModel.Event.OpenNfcSharingDialog)
        val store = ViewModelStore().apply { put("friends", viewModel) }
        store.clear()
        assertEquals(null, nfcSharingSession.current())
    }

    @Test
    fun `sharing cannot start before a username is available`() = runTest {
        setupViewModel("")
        viewModel.action.test {
            viewModel.trigger(FriendsViewModel.Event.OpenNfcSharingDialog)
            assertTrue(awaitItem() is FriendsViewModel.Action.Toast)
        }
        assertEquals(null, nfcSharingSession.current())
    }
}
