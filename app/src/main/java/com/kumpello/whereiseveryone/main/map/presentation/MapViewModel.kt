package com.kumpello.whereiseveryone.main.map.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.main.common.domain.model.Location
import com.kumpello.whereiseveryone.main.map.data.model.FriendData
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetPermissionsStatusUseCase
import com.kumpello.whereiseveryone.main.map.domain.usecase.UpdateStatusUseCase
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
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
import timber.log.Timber
import java.util.EnumMap

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel( //Rename to Main?
    private val locationService: LocationService,
    private val positionsService: PositionsService,
    private val saveKeyUseCase: SaveKeyUseCase,
    private val getKeyUseCase: GetKeyUseCase,
    private val updateStatusUseCase: UpdateStatusUseCase,
    private val getPermissionsStatusUseCase: GetPermissionsStatusUseCase
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
        viewModelScope.launch(Dispatchers.IO) {
            state.update { state ->
                state.copy(
                    userMessage = getKeyUseCase
                        .getValue(WhereIsEveryoneApplication.USER_MESSAGE_KEY)
                        .orEmpty()
                )
            }
        }
    }

    fun setPermissions(context: Context) {
        state.update {
            it.copy(
                permissionsState = getPermissionsStatusUseCase.execute(context)
            )
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.NavigateFriends -> navigateFriends()
            Event.NavigateSettings -> navigateSettings()
            Event.BackToMap -> backToMap()
            Event.CenterMap -> centerMap()
            Event.NavigateMessage -> navigateMessage()
            is Event.WriteMessage -> writeMessage(message = event.message)
            Event.SendMessage -> sendMessage()
            Event.OnPermissionAllow -> onPermissionAllow()
            Event.OnPermissionDeny -> onPermissionDeny()
        }
    }

    private fun onPermissionAllow() {
        viewModelScope.launch {
            _action.emit(Action.ShowPermissionSettings(state.value.permissionsState))
        }
        state.update {
            it.copy(
                permissionNotificationShown = true
            )
        }
    }

    private fun onPermissionDeny() {
        //TODO: ?
        state.update {
            it.copy(
                permissionNotificationShown = true
            )
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

    private suspend fun collectPositions() {
        positionsService.getFriendsFlow().collect { positions ->
            when (positions) {
                is PositionsResponse.FriendsData -> {
                    state.update { state ->
                        state.copy(
                            friends = positions.positions
                        )
                    }
                }

                is PositionsResponse.ErrorData -> {
                    //TODO: Toast!
                    Timber.d(positions.toString())
                }
            }
        }
    }

    private fun navigateSettings() {
        viewModelScope.launch {
            _action.emit(Action.NavigateSettings)
        }
    }

    private fun navigateMessage() {
        state.update {
            it.copy(
                screenState = ScreenState.Message
            )
        }
    }

    private fun writeMessage(message: String) {
        state.update {
            it.copy(
                userMessageField = message
            )
        }
    }

    private fun sendMessage() {
        viewModelScope.launch {

            when (updateStatusUseCase.execute(state.value.userMessageField)) {
                is CodeResponse.ErrorData -> {
                    //TODO: Toast!
                    Timber.d("Error updating message!")
                }

                is CodeResponse.SuccessNoContent -> {
                    val message = state.value.userMessageField
                    state.update { state ->
                        state.copy(
                            userMessage = message,
                            userMessageField = ""
                        )
                    }.also {
                        saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_MESSAGE_KEY, message)
                    }
                }
            }
        }
    }

    private fun navigateFriends() {
        viewModelScope.launch {
            _action.emit(Action.NavigateFriends)
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

    private fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            mapSettings = mapSettings,
            user = user,
            friends = friends,
            userMessage = userMessage,
            userMessageField = userMessageField,
            showPermissionNotification = !permissionNotificationShown && permissionsState.containsValue(false),
            permissions = permissionsState
        )
    }

    sealed class Action {
        data object CenterMap : Action()
        data object NavigateSettings : Action()
        data object NavigateFriends : Action()
        data class ShowPermissionSettings(val permissions: Map<String, Boolean>): Action()
    }

    sealed class Event {
        data object OnPermissionAllow: Event()
        data object OnPermissionDeny: Event()
        data object NavigateSettings : Event()
        data object NavigateFriends : Event()
        data object NavigateMessage : Event()
        data class WriteMessage(val message: String) : Event()
        data object SendMessage : Event()
        data object CenterMap : Event()
        data object BackToMap : Event()
        //data object LockMap: Command() //TODO: Add?
    }

    data class State(
        val permissionsState: Map<String, Boolean> = emptyMap(),
        val permissionNotificationShown: Boolean = false,
        val screenState: ScreenState = ScreenState.Map,
        val mapSettings: MapSettings = MapSettings(),
        val friends: List<FriendData> = emptyList(),
        val userMessage: String = "",
        val userMessageField: String = "",
        val user: Location = Location(
            lat = 0.0,
            lon = 0.0,
            bearing = 0.0f,
            alt = 0.0,
            accuracy = 0.0f,
        ), // TODO: Cache last location?
    )

    data class ViewState(
        val showPermissionNotification: Boolean,
        val permissions: Map<String, Boolean>,
        val screenState: ScreenState,
        val mapSettings: MapSettings,
        val user: Location,
        val friends: List<FriendData>,
        val userMessage: String,
        val userMessageField: String,
    )

}