package com.kumpello.whereiseveryone.main.map.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.main.common.domain.manager.FriendsManager
import com.kumpello.whereiseveryone.main.common.domain.usecase.CalculateBearingUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapFriendUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapLocationUseCase
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendLocalData
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.entity.LocationData
import com.kumpello.whereiseveryone.main.common.entity.toFriendState
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Clock
import kotlin.time.Instant

class MapViewModel(
    private val locationService: LocationService,
    private val friendsManager: FriendsManager,
    private val mapLocationUseCase: MapLocationUseCase,
    private val mapFriendUseCase: MapFriendUseCase,
    private val calculateBearingUseCase: CalculateBearingUseCase,
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
                                location = friendData.location?.let { loc ->
                                    LocationData(
                                        lat = loc.latitude,
                                        lon = loc.longitude,
                                        bearing = loc.bearing,
                                        alt = loc.altitude,
                                        accuracy = loc.accuracy,
                                        last_update = Instant.parse(loc.last_update),
                                    )
                                }
                            )
                        }
                        state.copy(friends = friends).toResult()
                    }

                    is FriendsResponse.ErrorData -> {
                        Timber.tag(TAG).d(response.toString())
                        state.toResult(SideEffect.Effect(Action.Toast(R.string.error_getting_friends)))
                    }
                }
            }

            Event.CenterMap -> {
                Timber.tag(TAG).d("Centering map, zoom: %s", state.mapSettings.zoom)
                state.toResult(SideEffect.Effect(Action.CenterMap(state.mapSettings.zoom)))
            }

            Event.ZoomIn -> {
                val newZoom = state.mapSettings.zoom + 0.5
                Timber.tag(TAG).d("Zooming in, new zoom: %s", newZoom)
                state.copy(mapSettings = state.mapSettings.copy(zoom = newZoom)).toResult(
                    SideEffect.Effect(Action.Zoom(newZoom))
                )
            }

            Event.ZoomOut -> {
                val newZoom = state.mapSettings.zoom - 0.5
                Timber.tag(TAG).d("Zooming out, new zoom: %s", newZoom)
                state.copy(mapSettings = state.mapSettings.copy(zoom = newZoom)).toResult(
                    SideEffect.Effect(Action.Zoom(newZoom))
                )
            }

            is Event.OnFriendClick -> state.copy(selectedFriend = event.friend).toResult()
            is Event.OnFriendLongClick -> state.copy(selectedFriend = event.friend).toResult()
            Event.DismissFriendDetails -> state.copy(selectedFriend = null).toResult()
            is Event.NavigateToFriend -> state.copy(
                navigatingFriend = event.friend,
                selectedFriend = null
            ).toResult()

            Event.CancelNavigation -> state.copy(navigatingFriend = null).toResult()
        }
    }

    override fun State.toViewState(): ViewState {
        val mappedUser = user?.let { mapLocationUseCase.execute(it) }
        val mappedFriends = friends.map { friend ->
            mapFriendUseCase.execute(friend, user)
        }
        val bearing = if (mappedUser != null && navigatingFriend != null) {
            navigatingFriend.location?.let { loc ->
                calculateBearingUseCase.execute(
                    mappedUser.lat,
                    mappedUser.lon,
                    loc.lat,
                    loc.lon
                )
            }
        } else null

        return ViewState(
            mapSettings = mapSettings,
            user = mappedUser,
            friends = mappedFriends,
            selectedFriend = selectedFriend,
            navigatingFriend = navigatingFriend,
            bearingToFriend = bearing
        )
    }

    sealed class Action {
        data class CenterMap(val zoom: Double) : Action()
        data class Zoom(val zoom: Double) : Action()
        data class Toast(@StringRes val id: Int) : Action()
    }

    sealed class Event {
        data class OnLocationUpdate(val location: LocationData?) : Event()
        data class OnFriendsUpdate(val response: FriendsResponse) : Event()
        data object ZoomOut : Event()
        data object ZoomIn : Event()
        data object CenterMap : Event()
        data class OnFriendClick(val friend: Friend) : Event()
        data class OnFriendLongClick(val friend: Friend) : Event()
        data object DismissFriendDetails : Event()
        data class NavigateToFriend(val friend: Friend) : Event()
        data object CancelNavigation : Event()
    }

    data class State(
        val selectedFriend: Friend? = null,
        val navigatingFriend: Friend? = null,
        val mapSettings: MapSettings = MapSettings(),
        val friends: List<FriendLocalData> = emptyList(),
        val user: LocationData? = null
    )

    @Immutable
    data class ViewState(
        val selectedFriend: Friend?,
        val navigatingFriend: Friend?,
        val bearingToFriend: Float?,
        val mapSettings: MapSettings,
        val user: Location?,
        val friends: List<Friend>,
    )

    companion object {
        private const val TAG = "MAP_VM"
    }
}
