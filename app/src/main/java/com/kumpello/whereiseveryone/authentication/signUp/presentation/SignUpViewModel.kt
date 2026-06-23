package com.kumpello.whereiseveryone.authentication.signUp.presentation

import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.PasswordValidationState
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.SignUpUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.ValidatePasswordUseCase
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import androidx.compose.runtime.Immutable
import timber.log.Timber

class SignUpViewModel(
    private val signUpUseCase: SignUpUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateLoginInputUseCase: ValidateLoginInputUseCase
) : BaseViewModel<SignUpViewModel.State, SignUpViewModel.ViewState, SignUpViewModel.Event, SignUpViewModel.Action>(
    State()
) {

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            Event.OnSignUpClick -> state.copy(signUpState = AsyncState.Loading()).toResult(
                SideEffect.AsyncWork {
                    try {
                        val response = signUpUseCase.execute(
                            username = state.username,
                            password = state.password
                        )

                        when (response) {
                            SignUpUseCase.Response.Success -> Event.OnSignUpResult(true)
                            SignUpUseCase.Response.Error -> Event.OnSignUpResult(false)
                        }
                    } catch (e: Exception) {
                        Event.OnSignUpResult(false, e)
                    }
                }
            )

            is Event.OnSignUpResult -> {
                if (event.success) {
                    Timber.tag(TAG).d("SignUp succeeded!")
                    state.copy(signUpState = AsyncState.Success(Unit))
                        .toResult(SideEffect.Effect(Action.NavigateMain))
                } else {
                    Timber.tag(TAG).e("SignUp failed!")
                    event.error?.let { Timber.tag(TAG).e(it) }
                    state.copy(signUpState = AsyncState.Error(event.error))
                        .toResult(SideEffect.Effect(Action.MakeToast("SignUp failed!")))
                }
            }

            Event.NavigateLogin -> state.toResult(SideEffect.Effect(Action.NavigateLogin))

            is Event.SetUsername -> state.copy(
                username = validateLoginInputUseCase.execute(event.username)
            ).toResult()

            is Event.SetPassword -> state.copy(
                password = event.password
            ).toResult()
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            username = username,
            password = password,
            passwordState = validatePasswordUseCase.execute(password),
            signUpState = signUpState
        )
    }

    sealed class Action {
        data class MakeToast(val string: String) : Action()
        data object NavigateMain : Action()
        data object NavigateLogin : Action()
    }

    sealed class Event {
        data object OnSignUpClick : Event()
        data class SetUsername(val username: String) : Event()
        data class SetPassword(val password: String) : Event()
        data object NavigateLogin : Event()
        data class OnSignUpResult(val success: Boolean, val error: Throwable? = null) : Event()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val username: String = "",
        val password: String = "",
        val passwordValidationState: PasswordValidationState = PasswordValidationState(),
        val signUpState: AsyncState<Unit> = AsyncState.Idle
    )

    @Immutable
    data class ViewState(
        val screenState: ScreenState,
        val username: String,
        val password: String,
        val passwordState: PasswordValidationState,
        val signUpState: AsyncState<Unit>
    )

    companion object {
        private const val TAG = "SIGN_UP_VM"
    }
}
