package com.kumpello.whereiseveryone.authentication.login.presentation

import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.login.domain.usecase.LoginUseCase
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import androidx.compose.runtime.Immutable
import timber.log.Timber

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val validateLoginInputUseCase: ValidateLoginInputUseCase
) : BaseViewModel<LoginViewModel.State, LoginViewModel.ViewState, LoginViewModel.Event, LoginViewModel.Action>(
    State()
) {

    override fun handleGlobalError(e: Exception) {
        Timber.tag(TAG).e(e, "Global error caught")
        if (e is java.io.IOException) {
            trigger(Event.OnLoginResult(false, e, "Server unreachable"))
        }
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            Event.OnLoginClick -> state.copy(loginState = AsyncState.Loading()).toResult(
                SideEffect.AsyncWork {
                    val response = loginUseCase.execute(
                        username = state.username,
                        password = state.password
                    )
                    when (response) {
                        LoginUseCase.Response.Success -> Event.OnLoginResult(true)
                        LoginUseCase.Response.Error -> Event.OnLoginResult(false)
                    }
                }
            )

            is Event.OnLoginResult -> {
                if (event.success) {
                    Timber.tag(TAG).d("Login succeeded!")
                    state.copy(loginState = AsyncState.Success(Unit))
                        .toResult(SideEffect.Effect(Action.NavigateMain))
                } else {
                    Timber.tag(TAG).e("Login failed!")
                    event.error?.let { Timber.tag(TAG).e(it) }

                    val toastMessage = if (event.message.isNotEmpty()) {
                        event.message
                    } else {
                        "Login failed!"
                    }

                    state.copy(loginState = AsyncState.Error(event.error))
                        .toResult(SideEffect.Effect(Action.MakeToast(toastMessage)))
                }
            }

            Event.NavigateSignUp -> state.toResult(SideEffect.Effect(Action.NavigateSignUp))

            is Event.SetUsername -> state.copy(
                username = validateLoginInputUseCase.execute(event.username)
            ).toResult()

            is Event.SetPassword -> state.copy(
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
            loginState = loginState
        )
    }

    sealed class Action {
        data class MakeToast(val string: String) : Action()
        data object NavigateMain : Action()
        data object NavigateSignUp : Action()
    }

    sealed class Event {
        data object OnLoginClick : Event()
        data class SetUsername(val username: String) : Event()
        data class SetPassword(val password: String) : Event()
        data object TogglePasswordVisibility : Event()
        data object NavigateSignUp : Event()
        data class OnLoginResult(
            val success: Boolean,
            val error: Throwable? = null,
            val message: String = ""
        ) : Event()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val username: String = "",
        val password: String = "",
        val passwordVisible: Boolean = false,
        val loginState: AsyncState<Unit> = AsyncState.Idle
    )

    @Immutable
    data class ViewState(
        val screenState: ScreenState,
        val username: String,
        val password: String,
        val passwordVisible: Boolean,
        val loginState: AsyncState<Unit>
    )

    companion object {
        private const val TAG = "LOGIN_VM"
    }
}
