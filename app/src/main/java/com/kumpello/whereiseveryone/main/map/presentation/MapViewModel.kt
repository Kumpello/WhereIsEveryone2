package com.kumpello.whereiseveryone.main.map.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
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
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Clock
import kotlin.time.Instant

class MapViewModel(
    private val locationService: LocationService,
    private val friendsManager: FriendsManager,
    private val saveKeyUseCase: SaveKeyUseCase,
    private val getKeyUseCase: GetKeyUseCase,
    private val updateStatusUseCase: UpdateStatusUseCase,
    private val getPermissionsStatusUseCase: GetPermissionsStatusUseCase,
    private val convertAccuracyUseCase: ConvertAccuracyUseCase,
    private val convertAltUseCase: ConvertAltUseCase,
    private val convertLastUpdateUseCase: ConvertLastUpdateUseCase
) : BaseViewModel<MapViewModel.State, MapViewModel.ViewState, MapViewModel.Event, MapViewModel.Action>(
    State()
) {

    init {
        viewModelScope.launch {
            locationService.observeLocation().collect { location ->
                trigger(Event.OnLocationUpdate(location?.let {
                    LocationData(
                        lat = it.latitude,
                        lon = it.longitude,
                        bearing = it.bearing,
                        alt = it.altitude,
                        accuracy = it.accuracy,
                        last_update = Clock.System.now(),
                    )
                }))
            }
        }
        viewModelScope.launch {
            friendsManager.observeFriends().collect { response ->
                trigger(Event.OnFriendsUpdate(response))
            }
        }
        trigger(Event.LoadUserMessage)
    }

    fun setPermissions(context: Context) {
        trigger(Event.SetPermissions(getPermissionsStatusUseCase.execute(context)))
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.OnLocationUpdate -> state.copy(user = event.location).toResult()
            is Event.OnFriendsUpdate -> {
                when (val response = event.response) {
                    is FriendsResponse.FriendsData -> {
                        val friends = response.positions.map { friendData ->
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
                                    last_update = Instant.parse(friendData.location.last_update),
                                )
                            )
                        }
                        state.copy(friends = friends).toResult()
                    }
                    is FriendsResponse.ErrorData -> {
                        Timber.d(response.toString())
                        state.toResult(SideEffect.Effect(Action.Toast(R.string.error_getting_friends)))
                    }
                }
            }

            Event.LoadUserMessage -> state.toResult(SideEffect.AsyncWork {
                val message = getKeyUseCase.getValue(WhereIsEveryoneApplication.USER_MESSAGE_KEY).orEmpty()
                Event.OnUserMessageLoaded(message)
            })

            is Event.OnUserMessageLoaded -> state.copy(userMessage = event.message).toResult()

            is Event.SetPermissions -> state.copy(permissionsState = event.permissions).toResult()

            Event.OnPermissionAllow -> state.copy(permissionNotificationShown = true).toResult(
                SideEffect.Effect(Action.ShowPermissionSettings(state.permissionsState))
            )

            Event.OnPermissionDeny -> state.copy(permissionNotificationShown = true).toResult()

            Event.NavigateSettings -> state.toResult(SideEffect.Effect(Action.NavigateSettings))
            Event.NavigateFriends -> state.toResult(SideEffect.Effect(Action.NavigateFriends))
            Event.NavigateMessage -> state.copy(screenState = ScreenState.Message).toResult()
            Event.BackToMap -> state.copy(screenState = ScreenState.Map).toResult()

            is Event.WriteMessage -> state.copy(userMessageField = event.message).toResult()

            Event.SendMessage -> state.toResult(SideEffect.AsyncWork {
                try {
                    val message = state.userMessageField
                    when (updateStatusUseCase.execute(message)) {
                        is CodeResponse.SuccessNoContent -> {
                            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_MESSAGE_KEY, message)
                            Event.OnMessageSent(message)
                        }
                        is CodeResponse.ErrorData -> {
                            Timber.d("Error updating message!")
                            Event.OnMessageError(R.string.error_updating_message)
                        }
                    }
                } catch (e: Exception) {
                    Timber.d("Error updating message!\n%s", e.message.toString())
                    Event.OnMessageError(R.string.error_updating_message)
                }
            })

            is Event.OnMessageSent -> state.copy(userMessage = event.message, userMessageField = "").toResult()
            is Event.OnMessageError -> state.toResult(SideEffect.Effect(Action.Toast(event.errorId)))

            Event.ClearMessage -> state.copy(userMessageField = "").toResult(SideEffect.InternalEvent(Event.SendMessage))

            Event.CenterMap -> state.toResult(SideEffect.Effect(Action.CenterMap(state.mapSettings.zoom)))

            Event.ZoomIn -> {
                val newZoom = state.mapSettings.zoom + 0.5
                state.copy(mapSettings = state.mapSettings.copy(zoom = newZoom)).toResult(
                    SideEffect.Effect(Action.Zoom(newZoom))
                )
            }

            Event.ZoomOut -> {
                val newZoom = state.mapSettings.zoom - 0.5
                state.copy(mapSettings = state.mapSettings.copy(zoom = newZoom)).toResult(
                    SideEffect.Effect(Action.Zoom(newZoom))
                )
            }

            is Event.OnFriendClick -> TODO()
            is Event.OnFriendLongClick -> TODO()
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            mapSettings = mapSettings,
            user = user?.let {
                Location(
                    lat = it.lat,
                    lon = it.lon,
                    bearing = it.bearing,
                    alt = AltDifference.SOMEWHAT_SAME,
                    accuracy = convertAccuracyUseCase.execute(it.accuracy),
                    lastUpdateTime = it.last_update.toString(),
                    lastUpdateAge = convertLastUpdateUseCase.execute(it.last_update),
                    rawAlt = 0.0,
                    rawAccuracy = 0.0f
                )
            },
            friends = friends.map { friend ->
                Friend(
                    username = friend.username,
                    status = friend.status,
                    state = friend.state,
                    location = Location(
                        lat = friend.location.lat,
                        lon = friend.location.lon,
                        bearing = friend.location.bearing,
                        alt = convertAltUseCase.execute(user?.alt, friend.location.alt),
                        accuracy = convertAccuracyUseCase.execute(friend.location.accuracy),
                        lastUpdateTime = friend.location.last_update.toString(),
                        lastUpdateAge = convertLastUpdateUseCase.execute(friend.location.last_update),
                        rawAlt = 0.0,
                        rawAccuracy = 0.0f
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
        data class Toast(@StringRes val id: Int) : Action()
    }

    sealed class Event {
        data class OnLocationUpdate(val location: LocationData?) : Event()
        data class OnFriendsUpdate(val response: FriendsResponse) : Event()
        data object LoadUserMessage : Event()
        data class OnUserMessageLoaded(val message: String) : Event()
        data class SetPermissions(val permissions: Map<String, Boolean>) : Event()
        data object OnPermissionAllow : Event()
        data object OnPermissionDeny : Event()
        data object NavigateSettings : Event()
        data object NavigateFriends : Event()
        data object NavigateMessage : Event()
        data class WriteMessage(val message: String) : Event()
        data object SendMessage : Event()
        data class OnMessageSent(val message: String) : Event()
        data class OnMessageError(@StringRes val errorId: Int) : Event()
        data object ClearMessage : Event()
        data object ZoomOut : Event()
        data object ZoomIn : Event()
        data object CenterMap : Event()
        data object BackToMap : Event()
        data class OnFriendClick(val friend: Friend) : Event()
        data class OnFriendLongClick(val friend: Friend) : Event()
    }

    data class State(
        val permissionsState: Map<String, Boolean> = emptyMap(),
        val permissionNotificationShown: Boolean = false,
        val screenState: ScreenState = ScreenState.Map,
        val mapSettings: MapSettings = MapSettings(),
        val friends: List<FriendLocalData> = emptyList(),
        val userMessage: String = "",
        val userMessageField: String = "",
        val user: LocationData? = null
    )

    @Immutable
    data class ViewState(
        val showPermissionNotification: Boolean,
        val permissions: Map<String, Boolean>,
        val screenState: ScreenState,
        val mapSettings: MapSettings,
        val user: Location?,
        val friends: List<Friend>,
        val userMessage: String,
        val userMessageField: String,
    )
}
