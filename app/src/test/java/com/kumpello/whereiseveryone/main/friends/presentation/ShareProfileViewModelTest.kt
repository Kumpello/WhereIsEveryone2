package com.kumpello.whereiseveryone.main.friends.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShareProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesManager: PreferencesManager = mockk()
    private lateinit var viewModel: ShareProfileViewModel

    private fun setupViewModel(username: String? = "testuser") {
        coEvery { preferencesManager.get(PreferencesKey.UserName) } returns username
        viewModel = ShareProfileViewModel(preferencesManager)
    }

    @Test
    fun `initial state loads username`() = runTest {
        setupViewModel("testuser")
        viewModel.state.test {
            assertEquals("testuser", awaitItem().username)
        }
    }

    @Test
    fun `OnNfcNotSupported triggers Toast action`() = runTest {
        setupViewModel()
        viewModel.action.test {
            viewModel.trigger(ShareProfileViewModel.Event.OnNfcNotSupported)
            val action = awaitItem()
            assertTrue(action is ShareProfileViewModel.Action.Toast)
            assertEquals(R.string.nfc_not_supported, (action as ShareProfileViewModel.Action.Toast).id)
        }
    }

    @Test
    fun `OnNfcDisabled triggers Toast action`() = runTest {
        setupViewModel()
        viewModel.action.test {
            viewModel.trigger(ShareProfileViewModel.Event.OnNfcDisabled)
            val action = awaitItem()
            assertTrue(action is ShareProfileViewModel.Action.Toast)
            assertEquals(R.string.nfc_disabled, (action as ShareProfileViewModel.Action.Toast).id)
        }
    }
}
