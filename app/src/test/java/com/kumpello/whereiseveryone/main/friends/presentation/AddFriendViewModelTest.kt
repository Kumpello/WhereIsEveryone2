package com.kumpello.whereiseveryone.main.friends.presentation

import android.net.Uri
import app.cash.turbine.test
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddFriendViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val addFriendUseCase: AddFriendUseCase = mockk()
    private lateinit var viewModel: AddFriendViewModel

    private fun setupViewModel() {
        viewModel = AddFriendViewModel(addFriendUseCase)
    }

    private fun link(
        username: String = "alice",
        scheme: String = "https",
        host: String = "where-is-everyone.com",
        segments: List<String> = listOf("addfriend", username)
    ): Uri = mockk {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.host } returns host
        every { pathSegments } returns segments
        every { lastPathSegment } returns segments.lastOrNull()
    }

    @Test
    fun `link waits for confirmation and dismissing it never sends a request`() = runTest {
        setupViewModel()
        viewModel.state.test {
            awaitItem()
            viewModel.trigger(AddFriendViewModel.Event.OnUriReceived(link()))
            val pending = awaitItem()
            assertEquals("alice", pending.pendingLinkedFriend)
            assertEquals("", pending.addFriendNick)
            runCurrent()
            coVerify(exactly = 0) { addFriendUseCase.execute(any()) }

            viewModel.trigger(AddFriendViewModel.Event.AddFriend)
            viewModel.trigger(AddFriendViewModel.Event.DismissLinkedFriend)
            assertNull(awaitItem().pendingLinkedFriend)
            viewModel.trigger(AddFriendViewModel.Event.ConfirmLinkedFriend("alice"))
            runCurrent()
            coVerify(exactly = 0) { addFriendUseCase.execute(any()) }
        }
    }

    @Test
    fun `confirmation sends only the displayed username once`() = runTest {
        val response = CompletableDeferred<CodeResponse>()
        coEvery { addFriendUseCase.execute(any()) } coAnswers { response.await() }
        setupViewModel()
        viewModel.state.test {
            awaitItem()
            viewModel.trigger(AddFriendViewModel.Event.OnUriReceived(link()))
            assertEquals("alice", awaitItem().pendingLinkedFriend)
            viewModel.trigger(AddFriendViewModel.Event.OnUriReceived(link("mallory")))
            viewModel.trigger(AddFriendViewModel.Event.ConfirmLinkedFriend("mallory"))
            runCurrent()
            expectNoEvents()
            coVerify(exactly = 0) { addFriendUseCase.execute(any()) }

            viewModel.trigger(AddFriendViewModel.Event.ConfirmLinkedFriend("alice"))
            val loading = awaitItem()
            assertTrue(loading.actionState.isLoading)
            assertNull(loading.pendingLinkedFriend)
            viewModel.trigger(AddFriendViewModel.Event.ConfirmLinkedFriend("alice"))
            viewModel.trigger(AddFriendViewModel.Event.AddFriend)
            viewModel.trigger(AddFriendViewModel.Event.OnUriReceived(link("mallory")))
            runCurrent()
            coVerify(exactly = 1) { addFriendUseCase.execute("alice") }
            coVerify(exactly = 0) { addFriendUseCase.execute("mallory") }
            response.complete(CodeResponse.SuccessNoContent)
            assertTrue(awaitItem().actionState is AsyncState.Idle)
        }
    }

    @Test
    fun `invalid links including arbitrary QR URLs cannot open confirmation or send requests`() = runTest {
        setupViewModel()
        viewModel.state.test {
            awaitItem()
            val invalidLinks = listOf(
                link(scheme = "http"),
                link(host = "example.com"),
                link(segments = listOf("addfriend")),
                link(segments = listOf("addfriend-other", "alice")),
                link(segments = listOf("addfriend", "alice", "extra")),
                link(username = " ")
            )
            invalidLinks.forEach { viewModel.trigger(AddFriendViewModel.Event.OnUriReceived(it)) }
            runCurrent()
            expectNoEvents()
            coVerify(exactly = 0) { addFriendUseCase.execute(any()) }
        }
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
