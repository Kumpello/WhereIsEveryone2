package com.kumpello.whereiseveryone.main.map.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.main.LocationService
import com.kumpello.whereiseveryone.main.PositionsService
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import com.kumpello.whereiseveryone.main.map.data.model.UserPosition
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    val locationService: LocationService,
    val positionsService: PositionsService
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

    init {
        positionsService.startFriendsUpdates()
    }

    private fun collectPositions() {

    }

    private fun navigateSettings() {
        _state.value = _state.value.copy(
            screenState = ScreenState.Settings
        )
    }

    private fun navigateFriends() {
        _state.value = _state.value.copy(
            screenState = ScreenState.Friends
        )
    }

    private fun backToMap() {
        _state.value = _state.value.copy(
            screenState = ScreenState.Map
        )
    }

    fun trigger(command: Command) {
        when (command) {
            Command.NavigateFriends -> navigateFriends()
            Command.NavigateSettings -> navigateSettings()
            Command.BackToMap -> backToMap()
        }
    }

    private fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            mapSettings = mapSettings
        )
    }

    sealed class Action {

    }

    sealed class Command {
        data object NavigateSettings: Command()
        data object NavigateFriends: Command()
        data object BackToMap: Command()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val mapSettings: MapSettings = MapSettings(),
        val friends: List<UserPosition> = listOf()
    )

    data class ViewState(
        val screenState: ScreenState,
        val mapSettings: MapSettings,
    )

}