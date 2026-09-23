package com.kumpello.whereiseveryone.authentication.signUp.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.authentication.common.domain.model.RememberedCredentials
import com.kumpello.whereiseveryone.authentication.common.domain.repository.RememberedCredentialsRepository
import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.PasswordValidationState
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.SignUpUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.ValidatePasswordUseCase
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

class SignUpViewModel(
    private val signUpUseCase: SignUpUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateLoginInputUseCase: ValidateLoginInputUseCase,
    private val rememberedCredentialsRepository: RememberedCredentialsRepository
) : BaseViewModel<SignUpViewModel.State, SignUpViewModel.ViewState, SignUpViewModel.Event, SignUpViewModel.Action>(
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

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            Event.OnSignUpClick -> if (!state.credentialsReady || state.signUpState.isLoading) {
                state.toResult()
            } else state.copy(signUpState = AsyncState.Loading()).toResult(
                SideEffect.AsyncWork {
                    try {
                        val response = signUpUseCase.execute(
                            username = state.username,
                            password = state.password
                        )

                        when (response) {
                            SignUpUseCase.Response.Success -> Event.OnSignUpResult(
                                true,
                                credentialsSaveFailed = !updateRememberedCredentials(state)
                            )
                            SignUpUseCase.Response.Error -> Event.OnSignUpResult(false)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Event.OnSignUpResult(false, e)
                    }
                }
            )

            is Event.OnSignUpResult -> {
                if (event.success) {
                    Timber.tag(TAG).d("SignUp succeeded!")
                    val effects = buildList<SideEffect<Event, Action>> {
                        if (event.credentialsSaveFailed) add(SideEffect.Effect(Action.CredentialsError))
                        add(SideEffect.Effect(Action.NavigateMain))
                    }
                    state.copy(signUpState = AsyncState.Success(Unit)).toResult(*effects.toTypedArray())
                } else {
                    if (event.error != null) {
                        Timber.tag(TAG).w(event.error, "Unable to sign up")
                    } else {
                        Timber.tag(TAG).d("Sign-up request rejected")
                    }
                    state.copy(signUpState = AsyncState.Error(event.error))
                        .toResult(SideEffect.Effect(Action.MakeToast("SignUp failed!")))
                }
            }

            is Event.OnRememberedCredentialsLoaded -> state.copy(
                rememberPassword = event.credentials != null,
                credentialsReady = true
            ).toResult(*if (event.failed) arrayOf(SideEffect.Effect(Action.CredentialsError)) else emptyArray())

            Event.ToggleRememberPassword -> when {
                !state.credentialsReady || state.signUpState.isLoading -> state.toResult()
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

            Event.NavigateLogin -> state.toResult(SideEffect.Effect(Action.NavigateLogin))

            is Event.SetUsername -> if (!state.credentialsReady || state.signUpState.isLoading) {
                state.toResult()
            } else state.copy(
                username = validateLoginInputUseCase.execute(event.username)
            ).toResult()

            is Event.SetPassword -> if (!state.credentialsReady || state.signUpState.isLoading) {
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
            passwordState = validatePasswordUseCase.execute(password),
            signUpState = signUpState
        )
    }

    sealed class Action {
        data object CredentialsError : Action()
        data class MakeToast(val string: String) : Action()
        data object NavigateMain : Action()
        data object NavigateLogin : Action()
    }

    sealed class Event {
        data class OnRememberedCredentialsLoaded(
            val credentials: RememberedCredentials?,
            val failed: Boolean = false
        ) : Event()
        data object ToggleRememberPassword : Event()
        data class OnRememberedCredentialsCleared(val success: Boolean) : Event()
        data object OnSignUpClick : Event()
        data class SetUsername(val username: String) : Event()
        data class SetPassword(val password: String) : Event()
        data object TogglePasswordVisibility : Event()
        data object NavigateLogin : Event()
        data class OnSignUpResult(
            val success: Boolean,
            val error: Throwable? = null,
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
        val passwordValidationState: PasswordValidationState = PasswordValidationState(),
        val signUpState: AsyncState<Unit> = AsyncState.Idle
    )

    @Immutable
    data class ViewState(
        val screenState: ScreenState,
        val username: String,
        val password: String,
        val passwordVisible: Boolean,
        val rememberPassword: Boolean = false,
        val credentialsReady: Boolean = true,
        val passwordState: PasswordValidationState,
        val signUpState: AsyncState<Unit>
    )

    companion object {
        private const val TAG = "SIGN_UP_VM"
    }
}
