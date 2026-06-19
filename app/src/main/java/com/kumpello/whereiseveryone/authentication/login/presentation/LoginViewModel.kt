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
) : BaseViewModel<LoginViewModel.State, LoginViewModel.ViewState, LoginViewModel.Command, LoginViewModel.Action>(
    State()
) {

    override fun reduce(state: State, event: Command): ReducerResult<State, Command, Action> {
        return when (event) {
            Command.OnLoginClick -> state.copy(loginState = AsyncState.Loading()).toResult(
                SideEffect.AsyncWork {
                    try {
                        val response = loginUseCase.execute(
                            username = state.username,
                            password = state.password
                        )
                        when (response) {
                            LoginUseCase.Response.Success -> Command.OnLoginResult(true)
                            LoginUseCase.Response.Error -> Command.OnLoginResult(false)
                        }
                    } catch (e: Exception) {
                        Command.OnLoginResult(false, e)
                    }
                }
            )

            is Command.OnLoginResult -> {
                if (event.success) {
                    Timber.tag(TAG).d("Login succeeded!")
                    state.copy(loginState = AsyncState.Success(Unit))
                        .toResult(SideEffect.Effect(Action.NavigateMain))
                } else {
                    Timber.tag(TAG).e("Login failed!")
                    event.error?.let { Timber.tag(TAG).e(it) }
                    state.copy(loginState = AsyncState.Error(event.error))
                        .toResult(SideEffect.Effect(Action.MakeToast("Login failed!")))
                }
            }

            Command.NavigateSignUp -> state.toResult(SideEffect.Effect(Action.NavigateSignUp))

            is Command.SetUsername -> state.copy(
                username = validateLoginInputUseCase.execute(event.username)
            ).toResult()

            is Command.SetPassword -> state.copy(
                password = event.password
            ).toResult()
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            username = username,
            password = password,
            loginState = loginState
        )
    }

    sealed class Action {
        data class MakeToast(val string: String) : Action()
        data object NavigateMain : Action()
        data object NavigateSignUp : Action()
    }

    sealed class Command {
        data object OnLoginClick : Command()
        data class SetUsername(val username: String) : Command()
        data class SetPassword(val password: String) : Command()
        data object NavigateSignUp : Command()
        data class OnLoginResult(val success: Boolean, val error: Throwable? = null) : Command()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val username: String = "",
        val password: String = "",
        val loginState: AsyncState<Unit> = AsyncState.Idle
    )

    @Immutable
    data class ViewState(
        val screenState: ScreenState,
        val username: String,
        val password: String,
        val loginState: AsyncState<Unit>
    )

    companion object {
        private const val TAG = "LOGIN_VM"
    }
}
