package com.kumpello.whereiseveryone.authentication.splash.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.usecase.RefreshTokenUseCase
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesManager: PreferencesManager = mockk()
    private val refreshTokenUseCase: RefreshTokenUseCase = mockk()

    private lateinit var viewModel: SplashViewModel

    private fun setupViewModel() {
        viewModel = SplashViewModel(preferencesManager, refreshTokenUseCase)
    }

    @Test
    fun `CheckUserStatus with no token navigates to SignUp`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns null
        setupViewModel()

        viewModel.action.test {
            viewModel.trigger(SplashViewModel.Event.CheckUserStatus(null))
            assertEquals(SplashViewModel.Action.NavigateSignUp, awaitItem())
        }
    }

    @Test
    fun `CheckUserStatus with valid token and refresh success navigates to Main`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "valid_token"
        coEvery { refreshTokenUseCase.execute() } returns RefreshTokenUseCase.Response.Success
        setupViewModel()

        viewModel.action.test {
            viewModel.trigger(SplashViewModel.Event.CheckUserStatus(null))
            assertEquals(SplashViewModel.Action.NavigateMain(null), awaitItem())
        }
    }

    @Test
    fun `CheckUserStatus with token and refresh error navigates to Login`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "valid_token"
        coEvery { refreshTokenUseCase.execute() } returns RefreshTokenUseCase.Response.Error
        setupViewModel()

        viewModel.action.test {
            viewModel.trigger(SplashViewModel.Event.CheckUserStatus(null))
            assertEquals(SplashViewModel.Action.NavigateLogin, awaitItem())
        }
    }
}
