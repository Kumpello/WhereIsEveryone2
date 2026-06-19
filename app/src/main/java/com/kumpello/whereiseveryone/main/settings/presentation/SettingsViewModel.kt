package com.kumpello.whereiseveryone.main.settings.presentation

import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.main.common.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceInterface
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import timber.log.Timber

class SettingsViewModel(
    @InjectedParam private val locationServiceInterface: LocationServiceInterface,
    private val wipeLocationUseCase: WipeLocationUseCase
) : BaseViewModel<SettingsViewModel.State, SettingsViewModel.ViewState, SettingsViewModel.Command, SettingsViewModel.Action>(
    State()
) {

    init {
        viewModelScope.launch(Dispatchers.Main) {
            LocationServiceImpl.stateFlow.collect { state ->
                trigger(Command.OnLocationServiceStateUpdate(state))
            }
        }
    }

    override fun reduce(state: State, event: Command): ReducerResult<State, Command, Action> {
        return when (event) {
            is Command.OnLocationServiceStateUpdate -> {
                Timber.tag(TAG).d("Location service state updated: %s", event.isRunning)
                state.copy(locationServiceState = event.isRunning).toResult()
            }

            Command.ClearData -> {
                Timber.tag(TAG).d("Clearing user location data")
                state.toResult(SideEffect.AsyncWork {
                    runCatching {
                        wipeLocationUseCase.execute()
                    }
                    // No event to return for now, maybe OnDataCleared in the future
                    Command.OnDataCleared
                })
            }

            Command.SwitchLocationServiceState -> {
                Timber.tag(TAG).d("Switching location service state, current: %s", state.locationServiceState)
                if (state.locationServiceState) {
                    locationServiceInterface.stopLocationService()
                } else {
                    locationServiceInterface.startLocationService()
                }
                state.toResult()
            }

            Command.OnDataCleared -> {
                Timber.tag(TAG).d("User data cleared successfully")
                state.toResult()
            }
        }
    }

    override fun State.toViewState(): ViewState {
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
        data object BackToMap : Action()
    }

    sealed class Command {
        data class OnLocationServiceStateUpdate(val isRunning: Boolean) : Command()
        data object ClearData : Command()
        data object SwitchLocationServiceState : Command()
        data object OnDataCleared : Command()
    }

    data class State(
        val locationServiceState: Boolean = true
    )

    @Immutable
    data class ViewState(
        val isLocationServiceRunning: Boolean,
        val locationSwitchText: String,
        val deleteLocationData: String,
    )

    companion object {
        private const val TAG = "SETTINGS_VM"
    }
}
