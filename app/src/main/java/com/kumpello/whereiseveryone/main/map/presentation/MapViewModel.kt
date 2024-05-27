package com.kumpello.whereiseveryone.main.map.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.main.common.domain.model.Location
import com.kumpello.whereiseveryone.main.map.data.model.FriendData
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    private val locationService: LocationService,
    private val positionsService: PositionsService,
    private val settingsViewModel: SettingsViewModel
) : ViewModel() {

    private val state = MutableStateFlow(State())
    val viewState: StateFlow<ViewState> = state.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = state.value.toViewState()
    )

    private val _action = MutableSharedFlow<Action>()
    val action: SharedFlow<Action> = _action.asSharedFlow()

    init {
        positionsService.startFriendsUpdates()
        viewModelScope.launch(Dispatchers.IO) {
            collectLocation()
            collectPositions()
        }
    }

    private suspend fun collectLocation() {
        locationService.getLocation().mapLatest { location ->
            Location(
                lat = location.latitude,
                lon = location.longitude,
                bearing = location.bearing,
                alt = location.altitude,
                accuracy = location.accuracy
            )
        }.collect { location ->
            state.update {
                it.copy(
                    user = location
                )
            }
        }
    }

    private fun collectPositions() {
        //TODO: Get friends updates
    }

    private fun navigateSettings() {

        state.update {
            it.copy(
                screenState = ScreenState.Settings(
                    settingsViewModel = settingsViewModel
                )
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

    private fun centerMap() {
        viewModelScope.launch {
            _action.emit(Action.CenterMap)
        }
    }

    fun trigger(command: Command) {
        when (command) {
            Command.NavigateFriends -> navigateFriends()
            Command.NavigateSettings -> navigateSettings()
            Command.BackToMap -> backToMap()
            Command.CenterMap -> centerMap()
        }
    }

    private fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            mapSettings = mapSettings,
            user = user
        )
    }

    sealed class Action {
        data object CenterMap: Action()
    }

    sealed class Command {
        data object NavigateSettings: Command()
        data object NavigateFriends: Command()
        data object CenterMap: Command()
        //data object LockMap: Command() //TODO: Add?
        data object BackToMap: Command()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val mapSettings: MapSettings = MapSettings(),
        val friends: List<FriendData> = emptyList(),
        val user: Location = Location(
            lat = 0.0,
            lon = 0.0,
            bearing = 0.0f,
            alt = 0.0,
            accuracy = 0.0f,
        ), // TODO: Cache last location?
    )

    data class ViewState(
        val screenState: ScreenState,
        val mapSettings: MapSettings,
        val user: Location,
        //TODO val friends: List<FriendPosition>,
        //TODO val userMessage: String
    )

}