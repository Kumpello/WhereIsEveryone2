package com.kumpello.whereiseveryone.main.map.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import com.kumpello.whereiseveryone.main.map.data.model.UserPosition
import com.kumpello.whereiseveryone.main.LocationService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    val locationService: LocationService
) : ViewModel() {

    private var _state = MutableStateFlow(State())
    val state: StateFlow<ViewState> = _state.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _state.value.toViewState()
    )
    lateinit var positions: MutableSharedFlow<PositionsResponse>

    private val _action = MutableSharedFlow<LoginViewModel.Action>()
    val action: SharedFlow<LoginViewModel.Action> = _action.asSharedFlow()

    private fun collectPositions() {

    }
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
        val friends: List<UserPosition> = listOf()
    )

    data class ViewState(
        val screenState: ScreenState
    )

}