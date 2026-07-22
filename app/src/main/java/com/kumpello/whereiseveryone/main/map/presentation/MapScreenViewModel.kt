package com.kumpello.whereiseveryone.main.map.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetPermissionsStatusUseCase
import kotlinx.coroutines.launch

class MapScreenViewModel(
    private val getPermissionsStatusUseCase: GetPermissionsStatusUseCase,
    private val locationService: LocationService
) : BaseViewModel<MapScreenViewModel.State, MapScreenViewModel.ViewState, MapScreenViewModel.Event, MapScreenViewModel.Action>(
    State()
) {

    init {
        viewModelScope.launch {
            locationService.observeForcedForeground().collect { enabled ->
                trigger(Event.OnForcedForegroundStatusChanged(enabled))
            }
        }
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.SetPermissions -> state.copy(permissionsState = event.permissions).toResult()

            Event.OnPermissionAllow -> state.copy(permissionNotificationShown = true).toResult(
                SideEffect.Effect(Action.ShowPermissionSettings(state.permissionsState))
            )

            Event.OnPermissionDeny -> state.copy(permissionNotificationShown = true).toResult()

            Event.NavigateSettings -> state.toResult(SideEffect.Effect(Action.NavigateSettings))
            Event.NavigateFriends -> state.toResult(SideEffect.Effect(Action.NavigateFriends))
            Event.NavigateMessage -> state.copy(screenState = ScreenState.Message).toResult()
            Event.BackToMap -> state.copy(screenState = ScreenState.Map).toResult()

            Event.FindMeClicked -> state.copy(showFindMeDialog = true).toResult()
            Event.DismissFindMeDialog -> state.copy(showFindMeDialog = false).toResult()

            is Event.EnableForcedForeground -> state.copy(showFindMeDialog = false).toResult(
                SideEffect.Effect(Action.Toast(R.string.forced_foreground_enabled_msg)),
                SideEffect.AsyncWork {
                    locationService.setForcedForeground(event.minutes)
                    Event.NoOp
                }
            )

            Event.DisableForcedForeground -> state.copy(showFindMeDialog = false).toResult(
                SideEffect.Effect(Action.Toast(R.string.forced_foreground_disabled_msg)),
                SideEffect.AsyncWork {
                    locationService.disableForcedForeground()
                    Event.NoOp
                }
            )

            is Event.OnForcedForegroundStatusChanged -> state.copy(isForcedForegroundEnabled = event.enabled).toResult()

            Event.NoOp -> state.toResult()
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            showPermissionNotification = !permissionNotificationShown && permissionsState.containsValue(
                false
            ),
            permissions = permissionsState,
            showFindMeDialog = showFindMeDialog,
            isForcedForegroundEnabled = isForcedForegroundEnabled
        )
    }

    fun getPermissions(context: android.content.Context): Map<String, Boolean> {
        return getPermissionsStatusUseCase.execute(context)
    }

    sealed class Action {
        data object NavigateSettings : Action()
        data object NavigateFriends : Action()
        data class ShowPermissionSettings(val permissions: Map<String, Boolean>) : Action()
        data class Toast(@androidx.annotation.StringRes val id: Int) : Action()
    }

    sealed class Event {
        data class SetPermissions(val permissions: Map<String, Boolean>) : Event()
        data object OnPermissionAllow : Event()
        data object OnPermissionDeny : Event()
        data object NavigateSettings : Event()
        data object NavigateFriends : Event()
        data object NavigateMessage : Event()
        data object BackToMap : Event()

        data object FindMeClicked : Event()
        data object DismissFindMeDialog : Event()
        data class EnableForcedForeground(val minutes: Int?) : Event()
        data object DisableForcedForeground : Event()
        data class OnForcedForegroundStatusChanged(val enabled: Boolean) : Event()
        data object NoOp : Event()
    }

    data class State(
        val permissionsState: Map<String, Boolean> = emptyMap(),
        val permissionNotificationShown: Boolean = false,
        val screenState: ScreenState = ScreenState.Map,
        val showFindMeDialog: Boolean = false,
        val isForcedForegroundEnabled: Boolean = false
    )

    @Immutable
    data class ViewState(
        val showPermissionNotification: Boolean,
        val permissions: Map<String, Boolean>,
        val screenState: ScreenState,
        val showFindMeDialog: Boolean,
        val isForcedForegroundEnabled: Boolean
    )
}
