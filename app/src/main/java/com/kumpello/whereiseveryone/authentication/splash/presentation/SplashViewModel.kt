package com.kumpello.whereiseveryone.authentication.splash.presentation

import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.RefreshTokenUseCase
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import androidx.compose.runtime.Immutable
import timber.log.Timber

class SplashViewModel(
    private val getCurrentAuthTokenUseCase: GetCurrentAuthTokenUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase
) : BaseViewModel<SplashViewModel.State, SplashViewModel.ViewState, SplashViewModel.Event, SplashViewModel.Action>(
    State
) {

    private suspend fun checkUserStatus() : AuthStatus {
        val authKey = getCurrentAuthTokenUseCase.execute()
        if (authKey.isNullOrEmpty()) {
            Timber.tag(TAG).d("No auth token found")
            return AuthStatus.NoToken
        }

        return when (refreshTokenUseCase.execute()) {
            RefreshTokenUseCase.Response.Success -> {
                Timber.tag(TAG).d("Token refresh successful")
                AuthStatus.RefreshSuccess
            }
            RefreshTokenUseCase.Response.Error -> {
                Timber.tag(TAG).d("Token refresh failed")
                AuthStatus.RefreshFailed
            }
        }
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.CheckUserStatus -> {
                Timber.tag(TAG).d("Checking user status from Splash, uri = %s", event.uri)
                state.toResult(
                    SideEffect.AsyncWork {
                        Event.OnAuthChecked(checkUserStatus(), event.uri)
                    }
                )
            }

            is Event.OnAuthChecked -> {
                Timber.tag(TAG).d("Auth checked: status = %s, uri = %s", event.status, event.uri)
                val action = when (event.status) {
                    AuthStatus.NoToken -> Action.NavigateSignUp
                    AuthStatus.RefreshSuccess -> Action.NavigateMain(event.uri)
                    AuthStatus.RefreshFailed -> Action.NavigateLogin
                }
                state.toResult(SideEffect.Effect(action))
            }
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState
    }

    sealed class Action {
        data object NavigateSignUp: Action()
        data class NavigateMain(val uri: android.net.Uri?): Action()
        data object NavigateLogin: Action()
    }

    sealed class Event {
        data class CheckUserStatus(val uri: android.net.Uri?) : Event()
        data class OnAuthChecked(val status: AuthStatus, val uri: android.net.Uri?) : Event()
    }

    sealed class AuthStatus {
        data object NoToken : AuthStatus()
        data object RefreshSuccess : AuthStatus()
        data object RefreshFailed : AuthStatus()
    }

    data object State

    @Immutable
    data object ViewState

    companion object {
        private const val TAG = "SPLASH_VM"
    }

}
