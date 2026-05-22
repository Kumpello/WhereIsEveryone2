package com.kumpello.whereiseveryone.main.map.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.main.common.domain.manager.FriendsManager
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAccuracyUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAltUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertLastUpdateUseCase
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendLocalData
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.entity.LocationData
import com.kumpello.whereiseveryone.main.common.entity.toFriendState
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel( //Rename to Main?
    private val locationService: LocationService,
    private val friendsManager: FriendsManager,
    private val saveKeyUseCase: SaveKeyUseCase,
    private val getKeyUseCase: GetKeyUseCase,
    private val updateStatusUseCase: UpdateStatusUseCase,
    private val getPermissionsStatusUseCase: GetPermissionsStatusUseCase,
    private val convertAccuracyUseCase: ConvertAccuracyUseCase,
    private val convertAltUseCase: ConvertAltUseCase,
    private val convertLastUpdateUseCase: ConvertLastUpdateUseCase
) : ViewModel() {

    private val locationFlow =
        locationService.observeLocation().mapLatest { location ->
            LocationData(
                lat = location.latitude,
                lon = location.longitude,
                bearing = location.bearing,
                alt = location.altitude,
                accuracy = location.accuracy,
                last_update = Clock.System.now(),
            )
        }

    val friendsFlow =
        friendsManager.observeFriends().mapLatest { response ->

            when (response) {

                is FriendsResponse.FriendsData -> {

                    response.positions.map { friendData ->

                        FriendLocalData(
                            username = friendData.username,
                            status = friendData.status,
                            state = friendData.state.toFriendState(),
                            location = LocationData(
                                lat = friendData.location.latitude,
                                lon = friendData.location.longitude,
                                bearing = friendData.location.bearing,
                                alt = friendData.location.altitude,
                                accuracy = friendData.location.accuracy,
                                last_update = Instant.parse(
                                    friendData.location.last_update
                                ),
                            )
                        )
                    }
                }

                is FriendsResponse.ErrorData -> {
                    //TODO: TOAST!
                    Timber.d(response.toString())
                    emptyList()
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val uiState = MutableStateFlow(State())
    private val combinedState =
        combine(
            locationFlow,
            friendsFlow,
            uiState
        ) { user, friends, state ->
            state.copy(
                user = user,
                friends = friends
            )
        }

    val viewState: StateFlow<ViewState> = combinedState.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = State().toViewState()
    )

    private val _action = MutableSharedFlow<Action>()
    val action: SharedFlow<Action> = _action.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.update { state ->
                state.copy(
                    userMessage = getKeyUseCase
                        .getValue(WhereIsEveryoneApplication.USER_MESSAGE_KEY)
                        .orEmpty()
                )
            }
        }
    }

    fun setPermissions(context: Context) {
        uiState.update {
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
            Event.ZoomIn -> zoomIn()
            Event.ZoomOut -> zoomOut()
            Event.NavigateMessage -> navigateMessage()
            is Event.WriteMessage -> writeMessage(message = event.message)
            Event.SendMessage -> sendMessage()
            Event.ClearMessage -> clearMessage()
            Event.OnPermissionAllow -> onPermissionAllow()
            Event.OnPermissionDeny -> onPermissionDeny()
        }
    }

    private fun onPermissionAllow() {
        viewModelScope.launch {
            _action.emit(Action.ShowPermissionSettings(uiState.value.permissionsState))
        }
        uiState.update {
            it.copy(
                permissionNotificationShown = true
            )
        }
    }

    private fun onPermissionDeny() {
        //TODO: ?
        uiState.update {
            it.copy(
                permissionNotificationShown = true
            )
        }
    }

    private fun navigateSettings() {
        viewModelScope.launch {
            _action.emit(Action.NavigateSettings)
        }
    }

    private fun navigateMessage() {
        uiState.update {
            it.copy(
                screenState = ScreenState.Message
            )
        }
    }

    private fun writeMessage(message: String) {
        uiState.update {
            it.copy(
                userMessageField = message
            )
        }
    }

    private fun sendMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val message = uiState.value.userMessageField
                when (updateStatusUseCase.execute(message)) {
                    is CodeResponse.ErrorData -> {
                        //TODO: Toast!
                        Timber.d("Error updating message!")
                    }

                    is CodeResponse.SuccessNoContent -> {
                        uiState.update { state ->
                            state.copy(
                                userMessage = message,
                                userMessageField = ""
                            )
                        }.also {
                            saveKeyUseCase.saveValue(
                                WhereIsEveryoneApplication.USER_MESSAGE_KEY,
                                message
                            )
                        }
                    }
                }
            }.onFailure { error ->
                //TODO: Toast!
                Timber.d("Error updating message!\n%s", error.message.toString())
            }
        }
    }

    private fun clearMessage() {
        uiState.update {
            it.copy(
                userMessageField = ""
            )
        }
        sendMessage()
    }

    private fun navigateFriends() {
        viewModelScope.launch {
            _action.emit(Action.NavigateFriends)
        }
    }

    private fun backToMap() {
        uiState.update {
            it.copy(
                screenState = ScreenState.Map
            )
        }
    }

    private fun centerMap() {
        viewModelScope.launch {
            _action.emit(Action.CenterMap(uiState.value.mapSettings.zoom))
        }
    }

    private fun zoomIn() {
        uiState.update {
            it.copy(
                mapSettings = it.mapSettings.copy(
                    zoom = it.mapSettings.zoom + 0.5
                )
            )
        }
        viewModelScope.launch {
            _action.emit(Action.Zoom(uiState.value.mapSettings.zoom))
        }
    }

    private fun zoomOut() {
        uiState.update {
            it.copy(
                mapSettings = it.mapSettings.copy(
                    zoom = it.mapSettings.zoom - 0.5
                )
            )
        }
        viewModelScope.launch {
            _action.emit(Action.Zoom(uiState.value.mapSettings.zoom))
        }
    }

    private fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            mapSettings = mapSettings,
            user = Location(
                lat = user.lat,
                lon = user.lon,
                bearing = user.bearing,
                alt = AltDifference.SOMEWHAT_SAME,
                accuracy = convertAccuracyUseCase.execute(user.accuracy),
                lastUpdateTime = user.last_update.toString(),
                lastUpdateAge = convertLastUpdateUseCase.execute(user.last_update)
            ),
            friends = friends.map { friend ->
                Friend(
                    username = friend.username,
                    status = friend.status,
                    state = friend.state,
                    location = Location(
                        lat = friend.location.lat,
                        lon = friend.location.lon,
                        bearing = friend.location.bearing,
                        alt = convertAltUseCase.execute(user.alt, friend.location.alt),
                        accuracy = convertAccuracyUseCase.execute(friend.location.accuracy),
                        lastUpdateTime = friend.location.last_update.toString(),
                        lastUpdateAge = convertLastUpdateUseCase.execute(friend.location.last_update)
                    )
                )
            },
            userMessage = userMessage,
            userMessageField = userMessageField,
            showPermissionNotification = !permissionNotificationShown && permissionsState.containsValue(
                false
            ),
            permissions = permissionsState
        )
    }

    sealed class Action {
        data class CenterMap(val zoom: Double) : Action()
        data class Zoom(val zoom: Double) : Action()
        data object NavigateSettings : Action()
        data object NavigateFriends : Action()
        data class ShowPermissionSettings(val permissions: Map<String, Boolean>) : Action()
    }

    sealed class Event {
        data object OnPermissionAllow : Event()
        data object OnPermissionDeny : Event()
        data object NavigateSettings : Event()
        data object NavigateFriends : Event()
        data object NavigateMessage : Event()
        data class WriteMessage(val message: String) : Event()
        data object SendMessage : Event()
        data object ClearMessage : Event()
        data object ZoomOut : Event()
        data object ZoomIn : Event()
        data object CenterMap : Event()
        data object BackToMap : Event()
        //data object LockMap: Command() //TODO: Add?
    }

    data class State(
        val permissionsState: Map<String, Boolean> = emptyMap(),
        val permissionNotificationShown: Boolean = false,
        val screenState: ScreenState = ScreenState.Map,
        val mapSettings: MapSettings = MapSettings(),
        val friends: List<FriendLocalData> = emptyList(),
        val userMessage: String = "",
        val userMessageField: String = "",
        val user: LocationData = LocationData(
            lat = 0.0,
            lon = 0.0,
            bearing = 0.0f,
            alt = 0.0,
            accuracy = 0.0f,
            last_update = Instant.DISTANT_PAST,
        ), // TODO: Cache last location?
    )

    data class ViewState(
        val showPermissionNotification: Boolean,
        val permissions: Map<String, Boolean>,
        val screenState: ScreenState,
        val mapSettings: MapSettings,
        val user: Location,
        val friends: List<Friend>,
        val userMessage: String,
        val userMessageField: String,
    )

}