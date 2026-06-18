package com.kumpello.whereiseveryone.authentication.splash.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.RefreshTokenUseCase
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import androidx.compose.runtime.Immutable

class SplashViewModel(
    private val getCurrentAuthTokenUseCase: GetCurrentAuthTokenUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase
) : BaseViewModel<SplashViewModel.State, SplashViewModel.ViewState, SplashViewModel.Command, SplashViewModel.Action>(
    State()
) {

    private suspend fun isUserLogged() : Boolean {
        val authKey = getCurrentAuthTokenUseCase.execute()
        if (authKey.isNullOrEmpty()) return false

        val result = refreshTokenUseCase.execute()
        return when (result) {
            RefreshTokenUseCase.Response.Success -> true
            RefreshTokenUseCase.Response.Error -> false
        }
    }

    override fun reduce(state: State, event: Command): ReducerResult<State, Command, Action> {
        return when (event) {
            Command.NavigateToNextDestination -> state.toResult(
                SideEffect.AsyncWork {
                    Command.OnAuthChecked(isUserLogged())
                }
            )
            is Command.OnAuthChecked -> state.toResult(
                SideEffect.Effect(if (event.isLogged) Action.NavigateMain else Action.NavigateSignUp)
            )
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            scale = scale,
        )
    }

    sealed class Action {
        data object NavigateSignUp: Action()
        data object NavigateMain: Action()

    }

    sealed class Command {
        data object NavigateToNextDestination : Command()
        data class OnAuthChecked(val isLogged: Boolean) : Command()
    }

    data class State(
        val scale : Animatable<Float, AnimationVector1D> = Animatable(0f)
    )

    @Immutable
    data class ViewState(
        val scale : Animatable<Float, AnimationVector1D>
    )

}
