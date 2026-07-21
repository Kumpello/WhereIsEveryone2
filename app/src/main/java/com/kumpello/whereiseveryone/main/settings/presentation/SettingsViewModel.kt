package com.kumpello.whereiseveryone.main.settings.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.main.common.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceInterface
import androidx.compose.runtime.Immutable
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.usecase.LogoutUseCase
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel.SideEffect.*
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel.Event.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import timber.log.Timber

class SettingsViewModel(
    @InjectedParam private val locationServiceInterface: LocationServiceInterface,
    private val wipeLocationUseCase: WipeLocationUseCase,
    private val preferencesManager: PreferencesManager,
    private val logoutUseCase: LogoutUseCase
) : BaseViewModel<SettingsViewModel.State, SettingsViewModel.ViewState, SettingsViewModel.Event, SettingsViewModel.Action>(
    State()
) {

    init {
        viewModelScope.launch(Dispatchers.Main) {
            LocationServiceImpl.stateFlow.collect { state ->
                trigger(OnLocationServiceStateUpdate(state))
            }
        }
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is OnLocationServiceStateUpdate -> {
                Timber.tag(TAG).d("Location service state updated: %s", event.isRunning)
                state.copy(locationServiceState = event.isRunning).toResult()
            }

            ClearData -> {
                Timber.tag(TAG).d("Clearing user location data and stopping service")
                state.toResult(AsyncWork {
                    try {
                        Timber.tag(TAG).d("Sending wipe location request to backend")
                        when (val response = wipeLocationUseCase.execute()) {
                            is CodeResponse.ErrorData -> {
                                Timber.tag(TAG).e("Error wiping location: %s", response.toString())
                                Toast(R.string.error_wiping_location)
                            }

                            CodeResponse.SuccessNoContent -> {
                                Timber.tag(TAG).d("Location wiped successfully")
                                locationServiceInterface.stopLocationService()
                                preferencesManager.save(PreferencesKey.LocationSharingEnabled, false)
                                OnDataCleared
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e("Exception when wiping location: %s", e.message)
                        Toast(R.string.error_wiping_location)
                    }
                })
            }

            SwitchLocationServiceState -> {
                val newState = !state.locationServiceState
                Timber.tag(TAG).d("Switching location service state, new state: %s", newState)
                if (state.locationServiceState) {
                    locationServiceInterface.stopLocationService()
                } else {
                    locationServiceInterface.startLocationService()
                }
                state.toResult(AsyncWork {
                    preferencesManager.save(PreferencesKey.LocationSharingEnabled, newState)
                    // We don't need to return a command here because LocationServiceImpl.stateFlow will trigger update
                    NoOp
                })
            }

            Logout -> {
                Timber.tag(TAG).d("Logging out user")
                state.toResult(AsyncWork {
                    locationServiceInterface.stopLocationService()
                    logoutUseCase.execute()
                    OnLogoutComplete
                })
            }

            OnLogoutComplete -> {
                Timber.tag(TAG).d("Logout complete, navigating to auth")
                state.toResult(Effect(Action.NavigateToAuth))
            }

            OnDataCleared -> {
                Timber.tag(TAG).d("User data cleared successfully")
                state.toResult(InternalEvent(Toast(R.string.location_wiped_correctly_sharing_stoped)))
            }

            is Toast -> state.toResult(Effect(Action.Toast(event.stringId)))

            NoOp -> state.toResult()
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            isLocationServiceRunning = locationServiceState,
            locationSwitchTextId = if (locationServiceState) {
                R.string.settings_stop_sharing_location
            } else {
                R.string.settings_start_sharing_location
            },
            deleteLocationDataId = R.string.settings_delete_location_data,
            logoutTextId = R.string.settings_logout
        )
    }

    sealed class Action {
        data object BackToMap : Action()
        data class Toast(@param:StringRes val id: Int) : Action()
        data object NavigateToAuth : Action()
    }

    sealed class Event {
        data class OnLocationServiceStateUpdate(val isRunning: Boolean) : Event()
        data object ClearData : Event()
        data object SwitchLocationServiceState : Event()
        data object Logout : Event()
        data object OnLogoutComplete : Event()
        data object OnDataCleared : Event()
        data class Toast(@param:StringRes val stringId: Int) : Event()
        data object NoOp : Event()
    }

    data class State(
        val locationServiceState: Boolean = true,
    )

    @Immutable
    data class ViewState(
        val isLocationServiceRunning: Boolean,
        @param:StringRes val locationSwitchTextId: Int,
        @param:StringRes val deleteLocationDataId: Int,
        @param:StringRes val logoutTextId: Int,
    )

    companion object {
        private const val TAG = "SETTINGS_VM"
    }
}
