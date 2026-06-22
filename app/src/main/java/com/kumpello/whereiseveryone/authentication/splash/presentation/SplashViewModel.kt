package com.kumpello.whereiseveryone.authentication.splash.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.RefreshTokenUseCase
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import androidx.compose.runtime.Immutable
import timber.log.Timber

class SplashViewModel(
    private val getCurrentAuthTokenUseCase: GetCurrentAuthTokenUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase
) : BaseViewModel<SplashViewModel.State, SplashViewModel.ViewState, SplashViewModel.Command, SplashViewModel.Action>(
    State()
) {

    private suspend fun isUserLogged() : Boolean {
        val authKey = getCurrentAuthTokenUseCase.execute()
        if (authKey.isNullOrEmpty()) {
            Timber.tag(TAG).d("No auth token found, user is not logged in")
            return false
        }

        val result = refreshTokenUseCase.execute()
        return when (result) {
            RefreshTokenUseCase.Response.Success -> {
                Timber.tag(TAG).d("Token refresh successful, user is logged in")
                true
            }
            RefreshTokenUseCase.Response.Error -> {
                Timber.tag(TAG).d("Token refresh failed, user is not logged in")
                false
            }
        }
    }

    override fun reduce(state: State, event: Command): ReducerResult<State, Command, Action> {
        return when (event) {
            Command.NavigateToNextDestination -> {
                Timber.tag(TAG).d("Navigating to next destination from Splash")
                state.toResult(
                    SideEffect.AsyncWork {
                        Command.OnAuthChecked(isUserLogged())
                    }
                )
            }

            is Command.OnAuthChecked -> {
                Timber.tag(TAG).d("Auth checked: logged in = %s", event.isLogged)
                state.toResult(
                    SideEffect.Effect(if (event.isLogged) Action.NavigateMain else Action.NavigateSignUp)
                )
            }
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

    companion object {
        private const val TAG = "SPLASH_VM"
    }

}
