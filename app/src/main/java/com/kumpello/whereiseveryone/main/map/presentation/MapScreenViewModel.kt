package com.kumpello.whereiseveryone.main.map.presentation

import androidx.compose.runtime.Immutable
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetPermissionsStatusUseCase

class MapScreenViewModel(
    private val getPermissionsStatusUseCase: GetPermissionsStatusUseCase
) : BaseViewModel<MapScreenViewModel.State, MapScreenViewModel.ViewState, MapScreenViewModel.Event, MapScreenViewModel.Action>(
    State()
) {

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
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            showPermissionNotification = !permissionNotificationShown && permissionsState.containsValue(
                false
            ),
            permissions = permissionsState
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
    }

    data class State(
        val permissionsState: Map<String, Boolean> = emptyMap(),
        val permissionNotificationShown: Boolean = false,
        val screenState: ScreenState = ScreenState.Map
    )

    @Immutable
    data class ViewState(
        val showPermissionNotification: Boolean,
        val permissions: Map<String, Boolean>,
        val screenState: ScreenState
    )
}
