package com.kumpello.whereiseveryone.authentication.signUp.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.PasswordValidationState
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.SignUpUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.ValidatePasswordUseCase
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

class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signUpUseCase: SignUpUseCase = mockk()
    private val validatePasswordUseCase: ValidatePasswordUseCase = mockk()
    private val validateLoginInputUseCase: ValidateLoginInputUseCase = mockk()

    private lateinit var viewModel: SignUpViewModel

    private fun setupViewModel() {
        viewModel = SignUpViewModel(signUpUseCase, validatePasswordUseCase, validateLoginInputUseCase)
    }

    @Test
    fun `initial state is correct`() = runTest {
        val expectedPasswordState = PasswordValidationState()
        every { validatePasswordUseCase.execute("") } returns expectedPasswordState
        setupViewModel()

        viewModel.state.test {
            val initialState = awaitItem()
            assertEquals("", initialState.username)
            assertEquals("", initialState.password)
            assertEquals(expectedPasswordState, initialState.passwordState)
            assertTrue(initialState.signUpState is AsyncState.Idle)
        }
    }

    @Test
    fun `setUsername updates state with validated input`() = runTest {
        every { validatePasswordUseCase.execute(any()) } returns PasswordValidationState()
        every { validateLoginInputUseCase.execute("user123!") } returns "user123"
        setupViewModel()

        viewModel.state.test {
            assertEquals("", awaitItem().username) // Initial
            viewModel.trigger(SignUpViewModel.Event.SetUsername("user123!"))
            assertEquals("user123", awaitItem().username)
        }
    }

    @Test
    fun `onSignUpClick success updates state and navigates`() = runTest {
        every { validatePasswordUseCase.execute(any()) } returns PasswordValidationState()
        coEvery { signUpUseCase.execute(any(), any()) } returns SignUpUseCase.Response.Success
        setupViewModel()

        viewModel.action.test {
            viewModel.state.test {
                assertTrue(awaitItem().signUpState is AsyncState.Idle) // Initial
                viewModel.trigger(SignUpViewModel.Event.OnSignUpClick)
                // Loading state
                assertTrue(awaitItem().signUpState is AsyncState.Loading)
                // Success state
                assertTrue(awaitItem().signUpState is AsyncState.Success)
            }
            assertEquals(SignUpViewModel.Action.NavigateMain, awaitItem())
        }
    }

    @Test
    fun `onSignUpClick failure updates state and shows toast`() = runTest {
        every { validatePasswordUseCase.execute(any()) } returns PasswordValidationState()
        coEvery { signUpUseCase.execute(any(), any()) } returns SignUpUseCase.Response.Error
        setupViewModel()

        viewModel.action.test {
            viewModel.state.test {
                assertTrue(awaitItem().signUpState is AsyncState.Idle) // Initial
                viewModel.trigger(SignUpViewModel.Event.OnSignUpClick)
                // Loading state
                assertTrue(awaitItem().signUpState is AsyncState.Loading)
                // Error state
                assertTrue(awaitItem().signUpState is AsyncState.Error)
            }
            val action = awaitItem()
            assertTrue(action is SignUpViewModel.Action.MakeToast)
            assertEquals("SignUp failed!", (action as SignUpViewModel.Action.MakeToast).string)
        }
    }
}
