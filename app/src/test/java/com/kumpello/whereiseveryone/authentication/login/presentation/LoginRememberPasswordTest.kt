package com.kumpello.whereiseveryone.authentication.login.presentation

import app.cash.turbine.test
import com.kumpello.whereiseveryone.authentication.common.domain.model.RememberedCredentials
import com.kumpello.whereiseveryone.authentication.common.domain.repository.RememberedCredentialsRepository
import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.login.domain.usecase.LoginUseCase
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
class LoginRememberPasswordTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val authUseCase = mockk<LoginUseCase>()
    private val credentials = mockk<RememberedCredentialsRepository>(relaxed = true)
    private val saved = MutableStateFlow<RememberedCredentials?>(null)

    private fun viewModel(): LoginViewModel {
        every { credentials.observe() } returns saved
        return LoginViewModel(authUseCase, ValidateLoginInputUseCase(), credentials)
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
            assertEquals("savedUser", state.username)
            assertEquals("savedPassword", state.password)
        }
    }

    @Test
    fun `opt in saves only after authentication succeeds and ignores edits during submission`() = runTest {
        val result = CompletableDeferred<LoginUseCase.Response>()
        coEvery { authUseCase.execute(any(), any()) } coAnswers { result.await() }
        val vm = viewModel()
        vm.action.test {
            vm.state.test {
                runCurrent()
                vm.trigger(LoginViewModel.Event.SetUsername("newUser"))
                vm.trigger(LoginViewModel.Event.SetPassword("newPassword123"))
                vm.trigger(LoginViewModel.Event.ToggleRememberPassword)
                vm.trigger(LoginViewModel.Event.OnLoginClick)
                runCurrent()
                coVerify(exactly = 0) { credentials.save(any(), any()) }
                vm.trigger(LoginViewModel.Event.SetPassword("changed"))
                vm.trigger(LoginViewModel.Event.ToggleRememberPassword)
                vm.trigger(LoginViewModel.Event.OnLoginClick)
                runCurrent()
                assertEquals("newPassword123", expectMostRecentItem().password)
                result.complete(LoginUseCase.Response.Success)
                runCurrent()
                assertTrue(expectMostRecentItem().loginState is AsyncState.Success)
                coVerify(exactly = 1) { authUseCase.execute("newUser", "newPassword123") }
                coVerify(exactly = 1) { credentials.save("newUser", "newPassword123") }
            }
            assertEquals(LoginViewModel.Action.NavigateMain, awaitItem())
        }
    }

    @Test
    fun `failed authentication does not persist credentials`() = runTest {
        coEvery { authUseCase.execute(any(), any()) } returns LoginUseCase.Response.Error
        val vm = viewModel()
        vm.state.test {
            runCurrent()
            vm.trigger(LoginViewModel.Event.ToggleRememberPassword)
            vm.trigger(LoginViewModel.Event.OnLoginClick)
            runCurrent()
            assertTrue(expectMostRecentItem().loginState is AsyncState.Error)
            coVerify(exactly = 0) { credentials.save(any(), any()) }
            coVerify(exactly = 0) { credentials.clear() }
        }
    }

    @Test
    fun `default choice does not save successful authentication`() = runTest {
        coEvery { authUseCase.execute(any(), any()) } returns LoginUseCase.Response.Success
        val vm = viewModel()
        vm.state.test {
            runCurrent()
            assertFalse(expectMostRecentItem().rememberPassword)
            vm.trigger(LoginViewModel.Event.OnLoginClick)
            runCurrent()
            assertTrue(expectMostRecentItem().loginState is AsyncState.Success)
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
            vm.trigger(LoginViewModel.Event.SetPassword("typedPassword"))
            vm.trigger(LoginViewModel.Event.ToggleRememberPassword)
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
            vm.trigger(LoginViewModel.Event.SetUsername("typedUser"))
            vm.trigger(LoginViewModel.Event.SetPassword("typedPassword"))
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
                vm.trigger(LoginViewModel.Event.ToggleRememberPassword)
                runCurrent()
                val state = expectMostRecentItem()
                assertTrue(state.rememberPassword)
                assertTrue(state.credentialsReady)
            }
            assertEquals(LoginViewModel.Action.CredentialsError, awaitItem())
        }
    }

    @Test
    fun `failed persistence reports error but completes successful authentication`() = runTest {
        coEvery { authUseCase.execute(any(), any()) } returns LoginUseCase.Response.Success
        coEvery { credentials.save(any(), any()) } throws IOException()
        val vm = viewModel()
        vm.action.test {
            vm.state.test {
                runCurrent()
                vm.trigger(LoginViewModel.Event.ToggleRememberPassword)
                vm.trigger(LoginViewModel.Event.OnLoginClick)
                runCurrent()
                assertTrue(expectMostRecentItem().loginState is AsyncState.Success)
            }
            assertEquals(LoginViewModel.Action.CredentialsError, awaitItem())
            assertEquals(LoginViewModel.Action.NavigateMain, awaitItem())
        }
    }
}
