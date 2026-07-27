package com.kumpello.whereiseveryone.main.friends.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddFriendViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val addFriendUseCase: AddFriendUseCase = mockk()
    private lateinit var viewModel: AddFriendViewModel

    private fun setupViewModel() {
        viewModel = AddFriendViewModel(addFriendUseCase)
    }

    @Test
    fun `setAddFriendNick updates state`() = runTest {
        setupViewModel()
        viewModel.state.test {
            assertEquals("", awaitItem().addFriendNick) // Initial
            viewModel.trigger(AddFriendViewModel.Event.SetAddFriendNick("nick123"))
            assertEquals("nick123", awaitItem().addFriendNick)
        }
    }

    @Test
    fun `AddFriend success updates state and shows toast`() = runTest {
        coEvery { addFriendUseCase.execute(any()) } returns CodeResponse.SuccessNoContent
        setupViewModel()

        viewModel.action.test {
            viewModel.state.test {
                awaitItem() // Initial
                viewModel.trigger(AddFriendViewModel.Event.AddFriend)
                assertTrue(awaitItem().actionState is AsyncState.Loading)
                assertTrue(awaitItem().actionState is AsyncState.Idle)
            }
            assertTrue(awaitItem() is AddFriendViewModel.Action.Toast)
            assertTrue(awaitItem() is AddFriendViewModel.Action.NotifyFriendAdded)
        }
    }

    @Test
    fun `AddFriend failure updates state and shows error toast`() = runTest {
        coEvery { addFriendUseCase.execute(any()) } returns CodeResponse.ErrorData(400, "Error", "Bad Request")
        setupViewModel()

        viewModel.action.test {
            viewModel.state.test {
                awaitItem() // Initial
                viewModel.trigger(AddFriendViewModel.Event.AddFriend)
                
                val next = awaitItem()
                if (next.actionState is AsyncState.Loading) {
                    assertTrue(awaitItem().actionState is AsyncState.Idle)
                } else {
                    assertTrue(next.actionState is AsyncState.Idle)
                }
            }
            assertTrue(awaitItem() is AddFriendViewModel.Action.Toast)
        }
    }

    @Test
    fun `ScanQrCode triggers OpenQrScanner action`() = runTest {
        setupViewModel()

        viewModel.action.test {
            viewModel.trigger(AddFriendViewModel.Event.ScanQrCode)
            assertTrue(awaitItem() is AddFriendViewModel.Action.OpenQrScanner)
        }
    }
}
