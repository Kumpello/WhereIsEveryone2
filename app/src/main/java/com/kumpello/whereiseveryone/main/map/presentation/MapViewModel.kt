package com.kumpello.whereiseveryone.main.map.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import com.kumpello.whereiseveryone.main.map.data.model.FriendPosition
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class MapViewModel(
    private val locationService: LocationService,
    private val positionsService: PositionsService
) : ViewModel() {

    private val state = MutableStateFlow(State())
    val viewState: StateFlow<ViewState> = state.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = state.value.toViewState()
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
        state.update {
            it.copy(
                screenState = ScreenState.Settings
            )
        }
    }

    private fun navigateFriends() {
        state.update {
            it.copy(
                screenState = ScreenState.Friends
            )
        }
    }

    private fun backToMap() {
        state.update {
            it.copy(
                screenState = ScreenState.Map
            )
        }
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
        val friends: List<FriendPosition> = listOf()
    )

    data class ViewState(
        val screenState: ScreenState,
        val mapSettings: MapSettings,
    )

}