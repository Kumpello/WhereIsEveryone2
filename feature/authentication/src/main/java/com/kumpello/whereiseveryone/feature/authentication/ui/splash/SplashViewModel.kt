package com.kumpello.whereiseveryone.feature.authentication.ui.splash

import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.session.RefreshTokenUseCase
import com.kumpello.whereiseveryone.data.restore.RestoreCredentialsManager
import com.kumpello.whereiseveryone.core.presentation.BaseViewModel
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class SplashViewModel(
    private val preferencesManager: PreferencesManager,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val restoreCredentialsManager: RestoreCredentialsManager
) : BaseViewModel<SplashViewModel.State, SplashViewModel.ViewState, SplashViewModel.Event, SplashViewModel.Action>(
    State
) {

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.CheckUserStatus -> {
                Timber.tag(TAG).d("Checking user status from Splash")
                state.toResult(
                    SideEffect.AsyncWork {
                        Event.OnAuthChecked(checkUserStatus(), event.uri)
                    }
                )
            }

            is Event.OnAuthChecked -> {
                Timber.tag(TAG).d("Auth checked: status = %s", event.status)
                val action = when (event.status) {
                    AuthStatus.NoToken -> Action.NavigateSignUp
                    AuthStatus.RefreshSuccess -> Action.NavigateMain(event.uri)
                    AuthStatus.RefreshFailed, AuthStatus.NetworkError -> Action.NavigateLogin
                }
                state.toResult(SideEffect.Effect(action))
            }
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState
    }

    private suspend fun checkUserStatus(): AuthStatus = try {
        val authKey = preferencesManager.get(PreferencesKey.AuthToken)
        if (authKey.isNullOrEmpty()) {
            if (restoreCredentialsManager.restoreSession()) AuthStatus.RefreshSuccess else AuthStatus.NoToken
        } else {
            when (refreshTokenUseCase.execute()) {
                RefreshTokenUseCase.Response.Success -> AuthStatus.RefreshSuccess
                RefreshTokenUseCase.Response.Error -> AuthStatus.RefreshFailed
                RefreshTokenUseCase.Response.NetworkError -> AuthStatus.NetworkError
            }
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Timber.tag(TAG).e(exception, "Unexpected error during authentication check")
        AuthStatus.NetworkError
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
        data object NetworkError : AuthStatus()
    }

    data object State

    @Immutable
    data object ViewState

    companion object {
        private const val TAG = "SPLASH_VM"
    }

}
