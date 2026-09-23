package com.kumpello.whereiseveryone.authentication.login.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.authentication.common.domain.model.RememberedCredentials
import com.kumpello.whereiseveryone.authentication.common.domain.repository.RememberedCredentialsRepository
import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.login.domain.usecase.LoginUseCase
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val validateLoginInputUseCase: ValidateLoginInputUseCase,
    private val rememberedCredentialsRepository: RememberedCredentialsRepository
) : BaseViewModel<LoginViewModel.State, LoginViewModel.ViewState, LoginViewModel.Event, LoginViewModel.Action>(
    State()
) {

    init {
        viewModelScope.launch {
            try {
                rememberedCredentialsRepository.observe().collect {
                    trigger(Event.OnRememberedCredentialsLoaded(it))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                trigger(Event.OnRememberedCredentialsLoaded(null, failed = true))
            }
        }
    }

    private suspend fun updateRememberedCredentials(state: State): Boolean {
        return try {
            if (state.rememberPassword) {
                rememberedCredentialsRepository.save(state.username, state.password)
            } else {
                rememberedCredentialsRepository.clear()
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Timber.tag(TAG).w("Unable to update remembered credentials")
            false
        }
    }

    override fun handleGlobalError(e: Exception) {
        if (e is java.io.IOException) {
            Timber.tag(TAG).w(e, "Login failed due to network error")
            trigger(Event.OnLoginResult(false, e, "Server unreachable"))
        } else {
            Timber.tag(TAG).e(e, "Unexpected login failure")
        }
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            Event.OnLoginClick -> if (!state.credentialsReady || state.loginState.isLoading) {
                state.toResult()
            } else state.copy(loginState = AsyncState.Loading()).toResult(
                SideEffect.AsyncWork {
                    val response = loginUseCase.execute(
                        username = state.username,
                        password = state.password
                    )
                    when (response) {
                        LoginUseCase.Response.Success -> Event.OnLoginResult(
                            true,
                            credentialsSaveFailed = !updateRememberedCredentials(state)
                        )
                        LoginUseCase.Response.Error -> Event.OnLoginResult(false)
                    }
                }
            )

            is Event.OnLoginResult -> {
                if (event.success) {
                    Timber.tag(TAG).d("Login succeeded!")
                    val effects = buildList<SideEffect<Event, Action>> {
                        if (event.credentialsSaveFailed) add(SideEffect.Effect(Action.CredentialsError))
                        add(SideEffect.Effect(Action.NavigateMain))
                    }
                    state.copy(loginState = AsyncState.Success(Unit)).toResult(*effects.toTypedArray())
                } else {
                    Timber.tag(TAG).d("Login failed")

                    val toastMessage = if (event.message.isNotEmpty()) {
                        event.message
                    } else {
                        "Login failed!"
                    }

                    state.copy(loginState = AsyncState.Error(event.error))
                        .toResult(SideEffect.Effect(Action.MakeToast(toastMessage)))
                }
            }

            is Event.OnRememberedCredentialsLoaded -> state.copy(
                username = if (state.credentialsInitialized) state.username else event.credentials?.username.orEmpty(),
                password = if (state.credentialsInitialized) state.password else event.credentials?.password.orEmpty(),
                rememberPassword = event.credentials != null,
                credentialsReady = true,
                credentialsInitialized = true
            ).toResult(*if (event.failed) arrayOf(SideEffect.Effect(Action.CredentialsError)) else emptyArray())

            Event.ToggleRememberPassword -> when {
                !state.credentialsReady || state.loginState.isLoading -> state.toResult()
                !state.rememberPassword -> state.copy(rememberPassword = true).toResult()
                else -> state.copy(rememberPassword = false, credentialsReady = false).toResult(
                    SideEffect.AsyncWork {
                        try {
                            rememberedCredentialsRepository.clear()
                            Event.OnRememberedCredentialsCleared(true)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            Event.OnRememberedCredentialsCleared(false)
                        }
                    }
                )
            }

            is Event.OnRememberedCredentialsCleared -> state.copy(
                rememberPassword = !event.success,
                credentialsReady = true
            ).toResult(*if (!event.success) arrayOf(SideEffect.Effect(Action.CredentialsError)) else emptyArray())

            Event.NavigateSignUp -> state.toResult(SideEffect.Effect(Action.NavigateSignUp))

            is Event.SetUsername -> if (!state.credentialsReady || state.loginState.isLoading) {
                state.toResult()
            } else state.copy(
                username = validateLoginInputUseCase.execute(event.username)
            ).toResult()

            is Event.SetPassword -> if (!state.credentialsReady || state.loginState.isLoading) {
                state.toResult()
            } else state.copy(
                password = event.password
            ).toResult()

            Event.TogglePasswordVisibility -> state.copy(
                passwordVisible = !state.passwordVisible
            ).toResult()
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            username = username,
            password = password,
            passwordVisible = passwordVisible,
            rememberPassword = rememberPassword,
            credentialsReady = credentialsReady,
            loginState = loginState
        )
    }

    sealed class Action {
        data object CredentialsError : Action()
        data class MakeToast(val string: String) : Action()
        data object NavigateMain : Action()
        data object NavigateSignUp : Action()
    }

    sealed class Event {
        data class OnRememberedCredentialsLoaded(
            val credentials: RememberedCredentials?,
            val failed: Boolean = false
        ) : Event()
        data object ToggleRememberPassword : Event()
        data class OnRememberedCredentialsCleared(val success: Boolean) : Event()
        data object OnLoginClick : Event()
        data class SetUsername(val username: String) : Event()
        data class SetPassword(val password: String) : Event()
        data object TogglePasswordVisibility : Event()
        data object NavigateSignUp : Event()
        data class OnLoginResult(
            val success: Boolean,
            val error: Throwable? = null,
            val message: String = "",
            val credentialsSaveFailed: Boolean = false
        ) : Event()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val username: String = "",
        val password: String = "",
        val passwordVisible: Boolean = false,
        val rememberPassword: Boolean = false,
        val credentialsReady: Boolean = false,
        val credentialsInitialized: Boolean = false,
        val loginState: AsyncState<Unit> = AsyncState.Idle
    )

    @Immutable
    data class ViewState(
        val screenState: ScreenState,
        val username: String,
        val password: String,
        val passwordVisible: Boolean,
        val rememberPassword: Boolean = false,
        val credentialsReady: Boolean = true,
        val loginState: AsyncState<Unit>
    )

    companion object {
        private const val TAG = "LOGIN_VM"
    }
}
