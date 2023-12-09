package com.kumpello.whereiseveryone.main.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.common.entities.ScreenState
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(

) : ViewModel() {
    private var _state = MutableStateFlow(State())
    val state: StateFlow<ViewState> = _state.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _state.value.toViewState()
    )

    private val _action = MutableSharedFlow<LoginViewModel.Action>()
    val action: SharedFlow<LoginViewModel.Action> = _action.asSharedFlow()

    fun trigger(command: Command) {
        when (command) {

            else -> {}
        }
    }

    private fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
        )
    }

    sealed class Action {

    }

    sealed class Command {
    }

    data class State(
        val screenState: ScreenState = ScreenState.Success,
    )

    data class ViewState(
        val screenState: ScreenState
    )
}