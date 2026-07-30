package com.kumpello.whereiseveryone.main.map.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.usecase.UpdateStatusUseCase
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MessageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val updateStatusUseCase: UpdateStatusUseCase = mockk()

    private lateinit var viewModel: MessageViewModel

    private fun setupViewModel(initialMessage: String = "Hello World") {
        coEvery { preferencesManager.get(PreferencesKey.UserMessage) } returns initialMessage
        viewModel = MessageViewModel(preferencesManager, updateStatusUseCase)
    }

    @Test
    fun `initial state loads user message from preferences`() = runTest {
        setupViewModel("Hello World")
        viewModel.state.test {
            assertEquals("Hello World", awaitItem().userMessage)
        }
    }

    @Test
    fun `WriteMessage updates userMessageField`() = runTest {
        setupViewModel()
        viewModel.state.test {
            awaitItem() // initial loaded state
            viewModel.trigger(MessageViewModel.Event.WriteMessage("New Status"))
            assertEquals("New Status", awaitItem().userMessageField)
        }
    }

    @Test
    fun `SendMessage success updates userMessage and notifies action`() = runTest {
        setupViewModel("Old Status")
        coEvery { updateStatusUseCase.execute("New Status") } returns CodeResponse.SuccessNoContent

        viewModel.action.test {
            viewModel.state.test {
                awaitItem() // Initial
                viewModel.trigger(MessageViewModel.Event.WriteMessage("New Status"))
                awaitItem()
                viewModel.trigger(MessageViewModel.Event.SendMessage)
                assertEquals("New Status", awaitItem().userMessage)
            }
            assertEquals(MessageViewModel.Action.NotifyMessageSent, awaitItem())
        }
    }

    @Test
    fun `SendMessage failure triggers Toast action`() = runTest {
        setupViewModel("Old Status")
        coEvery { updateStatusUseCase.execute("New Status") } returns CodeResponse.ErrorData(500, "Error", "Server Error")

        viewModel.action.test {
            viewModel.trigger(MessageViewModel.Event.WriteMessage("New Status"))
            viewModel.trigger(MessageViewModel.Event.SendMessage)

            val action = awaitItem()
            assertEquals(MessageViewModel.Action.Toast(R.string.error_updating_message), action)
        }
    }
}
