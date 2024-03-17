package com.kumpello.whereiseveryone.authentication.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.login.domain.usecase.LoginUseCase
import com.kumpello.whereiseveryone.common.entity.Response
import com.kumpello.whereiseveryone.common.entity.ScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val validateLoginInputUseCase: ValidateLoginInputUseCase
) : ViewModel() {

    private var _state = MutableStateFlow(State())
    val state: StateFlow<ViewState> =
        _state
            .map { state ->
                state.toViewState()
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = _state.value.toViewState()
            )

    private val _action = MutableSharedFlow<Action>()
    val action: SharedFlow<Action> = _action.asSharedFlow()

    private fun onLoginClick() {
        login()
    }

    private fun login() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = loginUseCase.execute(
                    username = _state.value.username,
                    password = _state.value.password
                )
                when (response) {
                    Response.Success -> onLoginSuccess()
                    Response.Error -> onLoginError(null)
                }
            }.onFailure { error ->
                onLoginError(error)
            }
        }
    }

    private suspend fun onLoginSuccess() {
        Timber.d("Login succeeded!")
        _action.emit(Action.NavigateMain)
    }

    private suspend fun onLoginError(throwable: Throwable?) {
        Timber.e("Login failed!")
        throwable.let { Timber.e(throwable) }
        _action.emit(Action.MakeToast("Login failed!"))
    }

    private fun navigateSignUp() {
        viewModelScope.launch {
            _action.emit(Action.NavigateSignUp)
        }
    }

    private fun setUsername(username: String) {
        _state.value = _state.value.copy(
            username = validateLoginInputUseCase.execute(username)
        )
    }

    private fun setPassword(password: String) {
        _state.value = _state.value.copy(
            password = password
        )
    }

    fun trigger(command: Command) {
        when (command) {
            Command.OnLoginClick -> onLoginClick()
            Command.NavigateSignUp -> navigateSignUp()
            is Command.SetUsername -> setUsername(command.username)
            is Command.SetPassword -> setPassword(command.password)
        }
    }

    private fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            username = username,
            password = password
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
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val username: String = "",
        val password: String = "",
    )

    data class ViewState(
        val screenState: ScreenState,
        val username: String,
        val password: String,
    )
}