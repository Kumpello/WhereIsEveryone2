package com.kumpello.whereiseveryone.feature.main.ui.settings

import com.kumpello.whereiseveryone.feature.main.R
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.core.presentation.BaseViewModel
import com.kumpello.whereiseveryone.feature.main.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.feature.main.location.LocationService
import androidx.compose.runtime.Immutable
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.session.LogoutUseCase
import com.kumpello.whereiseveryone.core.presentation.BaseViewModel.SideEffect.*
import com.kumpello.whereiseveryone.feature.main.ui.settings.SettingsViewModel.Event.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class SettingsViewModel(
    private val locationService: LocationService,
    private val wipeLocationUseCase: WipeLocationUseCase,
    private val preferencesManager: PreferencesManager,
    private val logoutUseCase: LogoutUseCase
) : BaseViewModel<SettingsViewModel.State, SettingsViewModel.ViewState, SettingsViewModel.Event, SettingsViewModel.Action>(
    State()
) {

    private val locationOperationMutex = Mutex()

    private val proximityDistanceFlow = MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch {
            combine(
                locationService.observeIsServiceRunning(),
                preferencesManager.observe(PreferencesKey.LocationSharingEnabled)
            ) { running, enabled -> OnLocationServiceStateUpdate(running, enabled != false) }.collect { event ->
                trigger(event)
            }
        }
        viewModelScope.launch {
            preferencesManager.observe(PreferencesKey.ProximityDistance).collect { distance ->
                if (proximityDistanceFlow.value == null) {
                    trigger(OnProximityDistanceUpdate(distance ?: 50))
                }
            }
        }
        viewModelScope.launch {
            proximityDistanceFlow
                .debounce(250.milliseconds)
                .distinctUntilChanged()
                .collect { distance ->
                    distance?.let {
                        Timber.tag(TAG).d("Debounced saving proximity distance: %d", it)
                        preferencesManager.save(PreferencesKey.ProximityDistance, it)
                    }
                }
        }
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is OnLocationServiceStateUpdate -> {
                Timber.tag(TAG).d("Location service state updated: %s", event.isRunning)
                state.copy(
                    locationServiceState = event.isRunning,
                    isSharingEnabled = event.isSharingEnabled
                ).toResult()
            }

            is OnProximityDistanceUpdate -> {
                state.copy(proximityDistance = event.distance).toResult()
            }

            is ChangeProximityDistance -> {
                proximityDistanceFlow.value = event.distance
                state.copy(proximityDistance = event.distance).toResult()
            }

            ClearData -> {
                Timber.tag(TAG).d("Clearing user location data and stopping service")
                state.toResult(locationWork {
                    try {
                        locationService.stopLocationService()
                        preferencesManager.save(PreferencesKey.LocationSharingEnabled, false)
                        Timber.tag(TAG).d("Sending wipe location request to backend")
                        when (val response = wipeLocationUseCase.execute()) {
                            is CodeResponse.ErrorData -> {
                                Timber.tag(TAG).d("Location wipe rejected")
                                Toast(R.string.error_wiping_location)
                            }

                            CodeResponse.SuccessNoContent -> {
                                Timber.tag(TAG).d("Location wiped successfully")
                                OnDataCleared
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Unable to wipe location")
                        Toast(R.string.error_wiping_location)
                    }
                })
            }

            SwitchLocationServiceState -> {
                val newState = !state.locationServiceState
                Timber.tag(TAG).d("Switching location service state, new state: %s", newState)
                state.toResult(locationWork {
                    if (!newState) locationService.stopLocationService()
                    preferencesManager.save(PreferencesKey.LocationSharingEnabled, newState)
                    if (newState) locationService.startLocationService()
                    NoOp
                })
            }

            ToggleSharing -> state.toResult(locationWork {
                if (locationService.observeIsServiceRunning().value) locationService.toggleSharing()
                NoOp
            })

            Logout -> {
                Timber.tag(TAG).d("Logging out user")
                state.toResult(locationWork {
                    locationService.stopLocationService()
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

    private fun locationWork(work: suspend () -> Event) = AsyncWork {
        locationOperationMutex.withLock { work() }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            isLocationServiceRunning = locationServiceState,
            isSharingEnabled = locationServiceState && isSharingEnabled,
            locationSwitchTextId = if (locationServiceState) {
                R.string.settings_stop_location_service
            } else {
                R.string.settings_start_location_service
            },
            sharingSwitchTextId = if (isSharingEnabled) {
                R.string.settings_stop_sharing_location
            } else {
                R.string.settings_start_sharing_location
            },
            deleteLocationDataId = R.string.settings_delete_location_data,
            logoutTextId = R.string.settings_logout,
            proximityDistance = proximityDistance
        )
    }

    sealed class Action {
        data object BackToMap : Action()
        data class Toast(@param:StringRes val id: Int) : Action()
        data object NavigateToAuth : Action()
    }

    sealed class Event {
        data class OnLocationServiceStateUpdate(val isRunning: Boolean, val isSharingEnabled: Boolean) : Event()
        data class OnProximityDistanceUpdate(val distance: Int) : Event()
        data class ChangeProximityDistance(val distance: Int) : Event()
        data object ClearData : Event()
        data object SwitchLocationServiceState : Event()
        data object ToggleSharing : Event()
        data object Logout : Event()
        data object OnLogoutComplete : Event()
        data object OnDataCleared : Event()
        data class Toast(@param:StringRes val stringId: Int) : Event()
        data object NoOp : Event()
    }

    data class State(
        val locationServiceState: Boolean = false,
        val isSharingEnabled: Boolean = true,
        val proximityDistance: Int = 50
    )

    @Immutable
    data class ViewState(
        val isLocationServiceRunning: Boolean,
        val isSharingEnabled: Boolean,
        @param:StringRes val locationSwitchTextId: Int,
        @param:StringRes val sharingSwitchTextId: Int,
        @param:StringRes val deleteLocationDataId: Int,
        @param:StringRes val logoutTextId: Int,
        val proximityDistance: Int
    )

    companion object {
        private const val TAG = "SETTINGS_VM"
    }
}
