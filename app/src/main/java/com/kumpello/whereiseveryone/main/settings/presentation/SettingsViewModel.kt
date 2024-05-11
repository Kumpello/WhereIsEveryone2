package com.kumpello.whereiseveryone.main.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.main.map.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val locationServiceInterface: LocationServiceInterface,
    private val wipeLocationUseCase: WipeLocationUseCase
) : ViewModel() {

    private var _state = MutableStateFlow(State())
    val state: StateFlow<ViewState> = _state.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _state.value.toViewState()
    )

    init {
        viewModelScope.launch(Dispatchers.Main) {
            LocationServiceImpl.stateFlow.collect { state ->
                _state.update {
                    it.copy(
                        locationServiceState = state
                    )
                }
            }
        }
    }

    private val _action = MutableSharedFlow<LoginViewModel.Action>()
    val action: SharedFlow<LoginViewModel.Action> = _action.asSharedFlow()

    fun trigger(command: Command) {
        when (command) {
            Command.ClearData -> clearData()
            Command.SwitchLocationServiceState -> switchLocationServiceState()
        }
    }

    private fun clearData() {
        viewModelScope.launch {
            runCatching {
                wipeLocationUseCase.execute()
            }
        }
    }

    private fun switchLocationServiceState() {
        when(state.value.isLocationServiceRunning) {
            true -> locationServiceInterface.stopLocationService()
            false -> locationServiceInterface.startLocationService()
        }
    }

    private fun State.toViewState(): ViewState {
        return ViewState(
            isLocationServiceRunning = locationServiceState,
            locationSwitchText = when(locationServiceState) {
                true -> "Stop sharing location"
                false -> "Start sharing location"
            },
            deleteLocationData = "Delete your location data"
        )
    }

    sealed class Action {
    }

    sealed class Command {
        data object ClearData : Command()
        data object SwitchLocationServiceState : Command()

    }

    data class State(
        val locationServiceState: Boolean = true
    )

    data class ViewState(
        val isLocationServiceRunning: Boolean,
        val locationSwitchText: String,
        val deleteLocationData: String,
    )
}