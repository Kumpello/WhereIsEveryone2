package com.kumpello.whereiseveryone.authentication.login.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.login.domain.usecase.LoginUseCase
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loginUseCase: LoginUseCase = mockk()
    private val validateLoginInputUseCase: ValidateLoginInputUseCase = mockk()

    private lateinit var viewModel: LoginViewModel

    private fun setupViewModel() {
        viewModel = LoginViewModel(loginUseCase, validateLoginInputUseCase)
    }

    @Test
    fun `initial state is correct`() = runTest {
        setupViewModel()
        viewModel.state.test {
            val initialState = awaitItem()
            assertEquals("", initialState.username)
            assertEquals("", initialState.password)
            assertTrue(initialState.loginState is AsyncState.Idle)
        }
    }

    @Test
    fun `setUsername updates state with validated input`() = runTest {
        every { validateLoginInputUseCase.execute("user123!") } returns "user123"
        setupViewModel()

        viewModel.state.test {
            assertEquals("", awaitItem().username) // Initial
            viewModel.trigger(LoginViewModel.Event.SetUsername("user123!"))
            assertEquals("user123", awaitItem().username)
        }
    }

    @Test
    fun `setPassword updates state`() = runTest {
        setupViewModel()

        viewModel.state.test {
            assertEquals("", awaitItem().password) // Initial
            viewModel.trigger(LoginViewModel.Event.SetPassword("password123"))
            assertEquals("password123", awaitItem().password)
        }
    }

    @Test
    fun `onLoginClick success updates state and navigates`() = runTest {
        coEvery { loginUseCase.execute(any(), any()) } returns LoginUseCase.Response.Success
        setupViewModel()

        viewModel.state.test {
            assertTrue(awaitItem().loginState is AsyncState.Idle) // Initial
            viewModel.trigger(LoginViewModel.Event.OnLoginClick)
            // Loading state
            assertTrue(awaitItem().loginState is AsyncState.Loading)
            // Success state
            assertTrue(awaitItem().loginState is AsyncState.Success)
        }

        viewModel.action.test {
            assertEquals(LoginViewModel.Action.NavigateMain, awaitItem())
        }
    }

    @Test
    fun `onLoginClick failure updates state and shows toast`() = runTest {
        coEvery { loginUseCase.execute(any(), any()) } returns LoginUseCase.Response.Error
        setupViewModel()

        viewModel.state.test {
            assertTrue(awaitItem().loginState is AsyncState.Idle) // Initial
            viewModel.trigger(LoginViewModel.Event.OnLoginClick)
            // Loading state
            assertTrue(awaitItem().loginState is AsyncState.Loading)
            // Error state
            assertTrue(awaitItem().loginState is AsyncState.Error)
        }

        viewModel.action.test {
            val action = awaitItem()
            assertTrue(action is LoginViewModel.Action.MakeToast)
            assertEquals("Login failed!", (action as LoginViewModel.Action.MakeToast).string)
        }
    }
}
