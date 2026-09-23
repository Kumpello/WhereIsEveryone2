package com.kumpello.whereiseveryone.authentication.signUp.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.authentication.common.domain.model.RememberedCredentials
import com.kumpello.whereiseveryone.authentication.common.domain.repository.RememberedCredentialsRepository
import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.SignUpUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.ValidatePasswordUseCase
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpRememberPasswordTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val authUseCase = mockk<SignUpUseCase>()
    private val credentials = mockk<RememberedCredentialsRepository>(relaxed = true)
    private val saved = MutableStateFlow<RememberedCredentials?>(null)

    private fun viewModel(): SignUpViewModel {
        every { credentials.observe() } returns saved
        return SignUpViewModel(authUseCase, ValidatePasswordUseCase(), ValidateLoginInputUseCase(), credentials)
    }

    @Test
    fun `restores remembered choice and appropriate fields`() = runTest {
        saved.value = RememberedCredentials("savedUser", "savedPassword")
        val vm = viewModel()
        vm.state.test {
            runCurrent()
            val state = expectMostRecentItem()
            assertTrue(state.credentialsReady)
            assertTrue(state.rememberPassword)
            assertEquals("", state.username)
            assertEquals("", state.password)
        }
    }

    @Test
    fun `opt in saves only after authentication succeeds and ignores edits during submission`() = runTest {
        val result = CompletableDeferred<SignUpUseCase.Response>()
        coEvery { authUseCase.execute(any(), any()) } coAnswers { result.await() }
        val vm = viewModel()
        vm.action.test {
            vm.state.test {
                runCurrent()
                vm.trigger(SignUpViewModel.Event.SetUsername("newUser"))
                vm.trigger(SignUpViewModel.Event.SetPassword("newPassword123"))
                vm.trigger(SignUpViewModel.Event.ToggleRememberPassword)
                vm.trigger(SignUpViewModel.Event.OnSignUpClick)
                runCurrent()
                coVerify(exactly = 0) { credentials.save(any(), any()) }
                vm.trigger(SignUpViewModel.Event.SetPassword("changed"))
                vm.trigger(SignUpViewModel.Event.ToggleRememberPassword)
                vm.trigger(SignUpViewModel.Event.OnSignUpClick)
                runCurrent()
                assertEquals("newPassword123", expectMostRecentItem().password)
                result.complete(SignUpUseCase.Response.Success)
                runCurrent()
                assertTrue(expectMostRecentItem().signUpState is AsyncState.Success)
                coVerify(exactly = 1) { authUseCase.execute("newUser", "newPassword123") }
                coVerify(exactly = 1) { credentials.save("newUser", "newPassword123") }
            }
            assertEquals(SignUpViewModel.Action.NavigateMain, awaitItem())
        }
    }

    @Test
    fun `failed authentication does not persist credentials`() = runTest {
        coEvery { authUseCase.execute(any(), any()) } returns SignUpUseCase.Response.Error
        val vm = viewModel()
        vm.state.test {
            runCurrent()
            vm.trigger(SignUpViewModel.Event.ToggleRememberPassword)
            vm.trigger(SignUpViewModel.Event.OnSignUpClick)
            runCurrent()
            assertTrue(expectMostRecentItem().signUpState is AsyncState.Error)
            coVerify(exactly = 0) { credentials.save(any(), any()) }
            coVerify(exactly = 0) { credentials.clear() }
        }
    }

    @Test
    fun `default choice does not save successful authentication`() = runTest {
        coEvery { authUseCase.execute(any(), any()) } returns SignUpUseCase.Response.Success
        val vm = viewModel()
        vm.state.test {
            runCurrent()
            assertFalse(expectMostRecentItem().rememberPassword)
            vm.trigger(SignUpViewModel.Event.OnSignUpClick)
            runCurrent()
            assertTrue(expectMostRecentItem().signUpState is AsyncState.Success)
            coVerify(exactly = 0) { credentials.save(any(), any()) }
            coVerify(exactly = 1) { credentials.clear() }
        }
    }

    @Test
    fun `opting out clears storage immediately but preserves typed fields`() = runTest {
        saved.value = RememberedCredentials("savedUser", "savedPassword")
        val vm = viewModel()
        vm.state.test {
            runCurrent()
            vm.trigger(SignUpViewModel.Event.SetPassword("typedPassword"))
            vm.trigger(SignUpViewModel.Event.ToggleRememberPassword)
            runCurrent()
            val state = expectMostRecentItem()
            assertFalse(state.rememberPassword)
            assertTrue(state.credentialsReady)
            assertEquals("typedPassword", state.password)
            coVerify(exactly = 1) { credentials.clear() }
        }
    }

    @Test
    fun `external removal updates choice without replacing typed fields`() = runTest {
        saved.value = RememberedCredentials("savedUser", "savedPassword")
        val vm = viewModel()
        vm.state.test {
            runCurrent()
            vm.trigger(SignUpViewModel.Event.SetUsername("typedUser"))
            vm.trigger(SignUpViewModel.Event.SetPassword("typedPassword"))
            saved.value = null
            runCurrent()
            val state = expectMostRecentItem()
            assertFalse(state.rememberPassword)
            assertEquals("typedUser", state.username)
            assertEquals("typedPassword", state.password)
        }
    }

    @Test
    fun `failed removal restores checked choice and reports error`() = runTest {
        saved.value = RememberedCredentials("savedUser", "savedPassword")
        coEvery { credentials.clear() } throws IOException()
        val vm = viewModel()
        vm.action.test {
            vm.state.test {
                runCurrent()
                vm.trigger(SignUpViewModel.Event.ToggleRememberPassword)
                runCurrent()
                val state = expectMostRecentItem()
                assertTrue(state.rememberPassword)
                assertTrue(state.credentialsReady)
            }
            assertEquals(SignUpViewModel.Action.CredentialsError, awaitItem())
        }
    }

    @Test
    fun `failed persistence reports error but completes successful authentication`() = runTest {
        coEvery { authUseCase.execute(any(), any()) } returns SignUpUseCase.Response.Success
        coEvery { credentials.save(any(), any()) } throws IOException()
        val vm = viewModel()
        vm.action.test {
            vm.state.test {
                runCurrent()
                vm.trigger(SignUpViewModel.Event.ToggleRememberPassword)
                vm.trigger(SignUpViewModel.Event.OnSignUpClick)
                runCurrent()
                assertTrue(expectMostRecentItem().signUpState is AsyncState.Success)
            }
            assertEquals(SignUpViewModel.Action.CredentialsError, awaitItem())
            assertEquals(SignUpViewModel.Action.NavigateMain, awaitItem())
        }
    }
}
