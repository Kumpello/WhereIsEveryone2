package com.kumpello.whereiseveryone.feature.main.ui.map

import com.kumpello.whereiseveryone.feature.main.location.LocationService
import com.kumpello.whereiseveryone.feature.main.R
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.core.presentation.BaseViewModel
import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.feature.main.ui.mapper.LocationUtils
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapFriendUseCase
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapLocationUseCase
import com.kumpello.whereiseveryone.feature.main.ui.model.Friend
import com.kumpello.whereiseveryone.data.model.FriendLocalData
import com.kumpello.whereiseveryone.feature.main.ui.model.Location
import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.model.toLocalData
import com.kumpello.whereiseveryone.data.network.model.SharingResponse
import com.kumpello.whereiseveryone.feature.main.domain.usecase.GetPausedFriendsUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.ResumeSharingUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.StopSharingUseCase
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MapViewModel(
    private val locationService: LocationService,
    private val friendsManager: FriendsStateRepository,
    private val mapLocationUseCase: MapLocationUseCase,
    private val mapFriendUseCase: MapFriendUseCase,
    private val stopSharingUseCase: StopSharingUseCase,
    private val resumeSharingUseCase: ResumeSharingUseCase,
    private val getPausedFriendsUseCase: GetPausedFriendsUseCase,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : BaseViewModel<MapViewModel.State, MapViewModel.ViewState, MapViewModel.Event, MapViewModel.Action>(
    State(),
    viewStateDispatcher = defaultDispatcher
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
                        speed = it.speed,
                        lastUpdate = System.currentTimeMillis(),
                    )
                }))
            }
        }
        viewModelScope.launch {
            try {
                friendsManager.observeFriends().collect { response ->
                    val event = when (response) {
                        is FriendsResponse.FriendsData -> {
                            val friends = withContext(defaultDispatcher) {
                                response.positions.map { it.toLocalData() }
                            }
                            Event.OnFriendsLoaded(friends)
                        }
                        is FriendsResponse.ErrorData -> Event.OnError(R.string.error_getting_friends)
                    }
                    trigger(event)
                    trigger(Event.CheckPaused)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Unable to observe friends")
                trigger(Event.OnError(R.string.error_getting_friends))
            }
        }
    }

    private suspend fun checkPaused(): Event {
        return try {
            when (val response = getPausedFriendsUseCase.execute()) {
                is SharingResponse.PausedFriends -> Event.OnPausedFriendsLoaded(response.usernames)
                is SharingResponse.ErrorData -> {
                    Timber.tag(TAG).d("Paused friends request rejected")
                    Event.NoOp
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Unable to load paused friends")
            Event.NoOp
        }
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.OnLocationUpdate -> state.copy(user = event.location).toResult()
            is Event.OnFriendsLoaded -> state.copy(friends = event.friends).toResult()

            is Event.OnPausedFriendsLoaded -> state.copy(pausedFriends = event.pausedFriends).toResult()

            is Event.ToggleSharing -> {
                val isPaused = state.pausedFriends.contains(event.nick)
                val (useCase, loadingMsg, successMsg, errorMsg) = if (isPaused) {
                    listOf(
                        resumeSharingUseCase::execute,
                        R.string.resume_sharing,
                        R.string.sharing_resumed_successfully,
                        R.string.error_resuming_sharing
                    )
                } else {
                    listOf(
                        stopSharingUseCase::execute,
                        R.string.stop_sharing,
                        R.string.sharing_stopped_successfully,
                        R.string.error_stopping_sharing
                    )
                }

                state.toResult(
                    SideEffect.Effect(Action.Toast(loadingMsg as Int)),
                    SideEffect.AsyncWork {
                        try {
                            val response =
                                (useCase as suspend (String) -> CodeResponse).invoke(
                                    event.nick
                                )
                            if (response is CodeResponse.SuccessNoContent) {
                                Event.OnActionSuccess(successMsg as Int)
                            } else {
                                Event.OnError(errorMsg as Int)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.tag(TAG).w(e, "Unable to change sharing")
                            Event.OnError(errorMsg as Int)
                        }
                    })
            }

            is Event.OnActionSuccess -> state.toResult(
                SideEffect.Effect(Action.Toast(event.messageId)),
                SideEffect.InternalEvent(Event.CheckPaused)
            )

            Event.CheckPaused -> state.toResult(SideEffect.AsyncWork { checkPaused() })

            Event.NoOp -> state.toResult()

            is Event.OnError -> state.toResult(SideEffect.Effect(Action.Toast(event.id)))

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
            is Event.OnCameraUpdate -> state.copy(
                mapSettings = state.mapSettings.copy(bearing = event.bearing)
            ).toResult()
        }
    }

    override fun State.toViewState(): ViewState {
        val mappedUser = user?.let { mapLocationUseCase.execute(it) }
        val mappedFriends = friends.map { friend ->
            mapFriendUseCase.execute(friend, user).copy(
                isPaused = pausedFriends.contains(friend.username)
            )
        }
        val bearing = if (mappedUser != null && navigatingFriend != null) {
            navigatingFriend.location?.let { loc ->
                val geographicBearing = LocationUtils.calculateBearing(
                    mappedUser.lat,
                    mappedUser.lon,
                    loc.lat,
                    loc.lon
                )
                ((geographicBearing - mapSettings.bearing + 360) % 360).toFloat()
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
        data class OnFriendsLoaded(val friends: List<FriendLocalData>) : Event()
        data class OnPausedFriendsLoaded(val pausedFriends: List<String>) : Event()
        data class ToggleSharing(val nick: String) : Event()
        data class OnActionSuccess(@StringRes val messageId: Int) : Event()
        data class OnError(@StringRes val id: Int) : Event()
        data object CheckPaused : Event()
        data object NoOp : Event()
        data object ZoomOut : Event()
        data object ZoomIn : Event()
        data object CenterMap : Event()
        data class OnFriendClick(val friend: Friend) : Event()
        data class OnFriendLongClick(val friend: Friend) : Event()
        data object DismissFriendDetails : Event()
        data class NavigateToFriend(val friend: Friend) : Event()
        data object CancelNavigation : Event()
        data class OnCameraUpdate(val bearing: Double) : Event()
    }

    data class State(
        val selectedFriend: Friend? = null,
        val navigatingFriend: Friend? = null,
        val mapSettings: MapSettings = MapSettings(),
        val friends: List<FriendLocalData> = emptyList(),
        val pausedFriends: List<String> = emptyList(),
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
