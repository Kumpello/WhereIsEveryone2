package com.kumpello.whereiseveryone.authentication.signUp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.SignUpUseCase
import com.kumpello.whereiseveryone.common.entities.Response
import com.kumpello.whereiseveryone.common.entities.ScreenState
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

//TODO: add validation to username and password!
//TODO: Password is saved in plaintext, some kind of encryption needs to be added!
class SignUpViewModel(
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private var _state = MutableStateFlow(State())
    val state: StateFlow<ViewState> = _state.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _state.value.toViewState()
    )

    private val _action = MutableSharedFlow<Action>()
    val action: SharedFlow<Action> = _action.asSharedFlow()

    private fun signUp() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = signUpUseCase.execute(
                    username = _state.value.username,
                    password = _state.value.password
                )

                when (response) {
                    Response.Success -> onSignUpSuccess()
                    Response.Error -> onSignUpError(null)
                }
            }.onFailure { error ->
                onSignUpError(error)
            }
        }
    }

    private suspend fun onSignUpSuccess() {
        Timber.d("SignUp succeeded!")
        _action.emit(Action.NavigateMain)
    }

    private suspend fun onSignUpError(throwable: Throwable?) {
        Timber.e("SignUp failed!")
        throwable.let { Timber.e(throwable) }
        _action.emit(Action.MakeToast("SignUp failed!"))
    }

    private fun navigateLogin() {
        viewModelScope.launch {
            _action.emit(Action.NavigateLogin)
        }
    }

    private fun setUsername(username: String) {
        _state.value = _state.value.copy(
            username = username
        )
    }

    private fun setPassword(password: String) {
        _state.value = _state.value.copy(
            password = password
        )
    }

    fun trigger(command: Command) {
        when (command) {
            Command.SignUp -> signUp()
            Command.NavigateLogin -> navigateLogin()
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
        data object NavigateLogin : Action()
    }

    sealed class Command {
        data object SignUp : Command()
        data class SetUsername(val username: String) : Command()
        data class SetPassword(val password: String) : Command()
        data object NavigateLogin : Command()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Success,
        val username: String = "",
        val password: String = ""
    )

    data class ViewState(
        val screenState: ScreenState,
        val username: String,
        val password: String
    )
}