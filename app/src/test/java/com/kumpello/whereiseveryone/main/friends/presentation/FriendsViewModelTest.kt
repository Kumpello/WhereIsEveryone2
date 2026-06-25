package com.kumpello.whereiseveryone.main.friends.presentation

import android.location.Location
import app.cash.turbine.test
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapFriendUseCase
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AcceptFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RejectFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.map.domain.model.FriendData
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import com.kumpello.whereiseveryone.main.map.domain.model.UserInfo
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private lateinit var viewModel: FriendsViewModel

    private fun setupViewModel() {
        viewModel = FriendsViewModel(
            removeFriendUseCase,
            getFriendsDataUseCase,
            acceptFriendUseCase,
            rejectFriendUseCase,
            locationService,
            mapFriendUseCase
        )
    }

    @Test
    fun `initial state triggers CheckFriends`() = runTest {
        coEvery { getFriendsDataUseCase.execute() } returns FriendsResponse.FriendsData(emptyList())
        setupViewModel()

        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue(initialState.friends.isEmpty())
        }
    }

    @Test
    fun `CheckFriends success updates friends list`() = runTest {
        val friendData = FriendData(
            username = "friend1",
            status = "status",
            state = "accepted",
            location = UserInfo(0.0, 0.0, null, null, null, "2023-01-01T00:00:00Z")
        )
        coEvery { getFriendsDataUseCase.execute() } returns FriendsResponse.FriendsData(listOf(friendData))
        val mappedFriend = mockk<Friend>()
        every { mapFriendUseCase.execute(any(), any()) } returns mappedFriend
        
        setupViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.friends.size)
            assertEquals(mappedFriend, state.friends[0])
        }
    }

    @Test
    fun `DeleteFriend success shows toast and rechecks friends`() = runTest {
        coEvery { getFriendsDataUseCase.execute() } returns FriendsResponse.FriendsData(emptyList())
        coEvery { removeFriendUseCase.execute("friend1") } returns CodeResponse.SuccessNoContent
        setupViewModel()

        viewModel.state.test {
            awaitItem() // Initial
            viewModel.trigger(FriendsViewModel.Event.DeleteFriend("friend1"))
            assertTrue(awaitItem().actionState is AsyncState.Loading)
            assertTrue(awaitItem().actionState is AsyncState.Idle)
        }

        viewModel.action.test {
            val action = awaitItem()
            assertTrue(action is FriendsViewModel.Action.Toast)
        }
    }

    @Test
    fun `AcceptFriend success shows toast and rechecks friends`() = runTest {
        coEvery { getFriendsDataUseCase.execute() } returns FriendsResponse.FriendsData(emptyList())
        coEvery { acceptFriendUseCase.execute("friend1") } returns CodeResponse.SuccessNoContent
        setupViewModel()

        viewModel.state.test {
            awaitItem() // Initial
            viewModel.trigger(FriendsViewModel.Event.AcceptFriend("friend1"))
            assertTrue(awaitItem().actionState is AsyncState.Loading)
            assertTrue(awaitItem().actionState is AsyncState.Idle)
        }

        viewModel.action.test {
            val action = awaitItem()
            assertTrue(action is FriendsViewModel.Action.Toast)
        }
    }
}
