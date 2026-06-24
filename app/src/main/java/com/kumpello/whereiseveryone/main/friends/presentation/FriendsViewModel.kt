package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.extension.formatDistance
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.main.common.domain.usecase.CalculateDistanceUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAccuracyUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAltUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertLastUpdateUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendLocalData
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.entity.LocationData
import com.kumpello.whereiseveryone.main.common.entity.toFriendState
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AcceptFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RejectFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Instant
import java.time.Instant as JavaInstant

class FriendsViewModel(
    private val addFriendUseCase: AddFriendUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase,
    private val getFriendsDataUseCase: GetFriendsDataUseCase,
    private val acceptFriendUseCase: AcceptFriendUseCase,
    private val rejectFriendUseCase: RejectFriendUseCase,
    private val convertAccuracyUseCase: ConvertAccuracyUseCase,
    private val convertAltUseCase: ConvertAltUseCase,
    private val convertLastUpdateUseCase: ConvertLastUpdateUseCase,
    private val locationService: LocationService,
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val getKeyUseCase: GetKeyUseCase
) : BaseViewModel<FriendsViewModel.State, FriendsViewModel.ViewState, FriendsViewModel.Event, FriendsViewModel.Action>(
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
                        last_update = Clock.System.now()
                    )
                }))
            }
        }
        viewModelScope.launch {
            val username = getKeyUseCase.getValue(WhereIsEveryoneApplication.USER_NAME_KEY)
            trigger(Event.OnUsernameLoaded(username ?: ""))
        }
        trigger(Event.CheckFriends)
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.OnLocationUpdate -> state.copy(userLocation = event.location).toResult()
            is Event.OnUriReceived -> {
                val username = event.uri.lastPathSegment
                if (username != null) {
                    trigger(Event.SetAddFriendNick(username))
                    trigger(Event.AddFriend)
                }
                state.toResult()
            }
            is Event.OnUsernameLoaded -> state.copy(username = event.username).toResult()
            Event.CheckFriends -> {
                Timber.tag(TAG).d("Checking friends")
                state.toResult(SideEffect.AsyncWork {
                    try {
                        when (val friends = getFriendsDataUseCase.execute()) {
                            is FriendsResponse.FriendsData -> {
                                val friendList = friends.positions.map { friendData ->
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
                                            last_update = Instant.parse(friendData.location.last_update)
                                        )
                                    )
                                }
                                Event.OnFriendsLoaded(friendList)
                            }

                            is FriendsResponse.ErrorData -> {
                                Timber.tag(TAG).d("Error getting friends!\n%s", friends)
                                Event.OnError(R.string.error_getting_friends)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Error getting friends!\n%s", e.message.toString())
                        Event.OnError(R.string.error_getting_friends)
                    }
                })
            }

            is Event.OnFriendsLoaded -> state.copy(friends = event.friends).toResult()
            is Event.OnError -> state.copy(actionState = AsyncState.Idle).toResult(SideEffect.Effect(Action.Toast(event.id)))

            is Event.SetAddFriendNick -> state.copy(addFriendNick = event.nick).toResult()

            Event.AddFriend -> {
                Timber.tag(TAG).d("Adding friend: %s", state.addFriendNick)
                state.copy(actionState = AsyncState.Loading(message = "Adding friend...")).toResult(SideEffect.AsyncWork {
                    try {
                        when (val response = addFriendUseCase.execute(state.addFriendNick)) {
                            CodeResponse.SuccessNoContent -> {
                                Event.OnActionSuccess(R.string.friend_added)
                            }

                            is CodeResponse.ErrorData -> {
                                Timber.tag(TAG).e(response.toString())
                                Event.OnError(R.string.error_adding_friend)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e("Error adding friend!\n%s", e.toString())
                        Event.OnError(R.string.error_adding_friend)
                    }
                })
            }

            is Event.DeleteFriend -> {
                Timber.tag(TAG).d("Deleting friend: %s", event.nick)
                state.copy(actionState = AsyncState.Loading(message = "Deleting friend...")).toResult(SideEffect.AsyncWork {
                    try {
                        when (val response = removeFriendUseCase.execute(event.nick)) {
                            CodeResponse.SuccessNoContent -> {
                                Event.OnActionSuccess(R.string.friend_deleted_successfully)
                            }

                            is CodeResponse.ErrorData -> {
                                Event.OnError(R.string.error_deleting_friend)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Error deleting friend!\n%s", e.message.toString())
                        Event.OnError(R.string.error_deleting_friend)
                    }
                })
            }

            is Event.AcceptFriend -> {
                Timber.tag(TAG).d("Accepting friend: %s", event.nick)
                state.copy(actionState = AsyncState.Loading(message = "Accepting request...")).toResult(SideEffect.AsyncWork {
                    try {
                        when (val response = acceptFriendUseCase.execute(event.nick)) {
                            CodeResponse.SuccessNoContent -> {
                                Event.OnActionSuccess(R.string.friend_accepted)
                            }

                            is CodeResponse.ErrorData -> {
                                Event.OnError(R.string.error_occurred_during_accepting_friend)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Error accepting friend!\n%s", e.message.toString())
                        Event.OnError(R.string.error_occurred_during_accepting_friend)
                    }
                })
            }

            is Event.RejectFriend -> {
                Timber.tag(TAG).d("Rejecting friend: %s", event.nick)
                state.copy(actionState = AsyncState.Loading(message = "Rejecting request...")).toResult(SideEffect.AsyncWork {
                    try {
                        when (val response = rejectFriendUseCase.execute(event.nick)) {
                            CodeResponse.SuccessNoContent -> {
                                Event.OnActionSuccess(R.string.rejected_successfully)
                            }

                            is CodeResponse.ErrorData -> {
                                Event.OnError(R.string.error_during_reject)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Error rejecting friend!\n%s", e.message.toString())
                        Event.OnError(R.string.error_during_reject)
                    }
                })
            }

            is Event.OnActionSuccess -> state.copy(actionState = AsyncState.Idle).toResult(
                SideEffect.Effect(Action.Toast(event.messageId)),
                SideEffect.InternalEvent(Event.CheckFriends)
            )

            is Event.OpenDeleteFriendDialog -> state.copy(
                deleteFriendDialogState = DeleteFriendDialogState.Open(event.friend)
            ).toResult()

            Event.CloseDeleteFriendDialog -> state.copy(
                deleteFriendDialogState = DeleteFriendDialogState.Closed
            ).toResult()

            is Event.SelectFriend -> state.copy(selectedFriend = event.friend).toResult()
            Event.ClearSelectedFriend -> state.copy(selectedFriend = null).toResult()
            Event.OpenShareDialog -> state.copy(isShareDialogOpen = true).toResult()
            Event.CloseShareDialog -> state.copy(isShareDialogOpen = false).toResult()
            Event.ShareViaNfc -> state.toResult(SideEffect.Effect(Action.TriggerNfcSharing(state.username)))
            Event.OnNfcNotSupported -> state.toResult(SideEffect.Effect(Action.Toast(R.string.nfc_not_supported)))
            Event.OnNfcDisabled -> state.toResult(SideEffect.Effect(Action.Toast(R.string.nfc_disabled)))
        }
    }

    override fun State.toViewState(): ViewState {
        val mappedFriends = friends.map { friend ->
            val dist = userLocation?.let {
                calculateDistanceUseCase.execute(
                    it.lat, it.lon, it.alt,
                    friend.location.lat, friend.location.lon, friend.location.alt
                )
            }
            Friend(
                username = friend.username,
                status = friend.status,
                state = friend.state,
                location = Location(
                    lat = friend.location.lat,
                    lon = friend.location.lon,
                    bearing = friend.location.bearing,
                    alt = convertAltUseCase.execute(userLocation?.alt, friend.location.alt),
                    rawAlt = friend.location.alt,
                    accuracy = convertAccuracyUseCase.execute(friend.location.accuracy),
                    rawAccuracy = friend.location.accuracy,
                    lastUpdateTime = formatLastUpdate(friend.location.last_update),
                    lastUpdateAge = convertLastUpdateUseCase.execute(friend.location.last_update)
                ),
                distance = dist,
                formattedDistance = dist?.let { formatDistance(it) }
            )
        }.sortedBy { it.distance ?: Double.MAX_VALUE }

        return ViewState(
            friends = mappedFriends,
            addFriendNick = addFriendNick,
            deleteFriendDialogState = deleteFriendDialogState,
            selectedFriend = selectedFriend,
            actionState = actionState,
            isShareDialogOpen = isShareDialogOpen,
            username = username,
        )
    }

    private fun formatLastUpdate(instant: Instant): String {
        val javaInstant = JavaInstant.parse(instant.toString())
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
            .withZone(ZoneId.systemDefault())
        return formatter.format(javaInstant)
    }

    sealed class Action {
        data class Toast(@StringRes val id: Int) : Action()
        data object BackToMap : Action()
        data class TriggerNfcSharing(val username: String?) : Action()
    }

    sealed class Event {
        data class OnLocationUpdate(val location: LocationData?) : Event()
        data class OnUriReceived(val uri: android.net.Uri) : Event()
        data class OnUsernameLoaded(val username: String) : Event()
        data object CheckFriends : Event()
        data class OnFriendsLoaded(val friends: List<FriendLocalData>) : Event()
        data class OnError(@StringRes val id: Int) : Event()
        data class SetAddFriendNick(val nick: String) : Event()
        data object AddFriend : Event()
        data class DeleteFriend(val nick: String): Event()
        data class AcceptFriend(val nick: String): Event()
        data class RejectFriend(val nick: String): Event()
        data class OpenDeleteFriendDialog(val friend: Friend): Event()
        data object CloseDeleteFriendDialog: Event()
        data class SelectFriend(val friend: Friend): Event()
        data object ClearSelectedFriend: Event()
        data class OnActionSuccess(@StringRes val messageId: Int) : Event()
        data object OpenShareDialog : Event()
        data object CloseShareDialog : Event()
        data object ShareViaNfc : Event()
        data object OnNfcNotSupported : Event()
        data object OnNfcDisabled : Event()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val friends: List<FriendLocalData> = emptyList(),
        val addFriendNick: String = "",
        val deleteFriendDialogState: DeleteFriendDialogState = DeleteFriendDialogState.Closed,
        val selectedFriend: Friend? = null,
        val userLocation: LocationData? = null,
        val actionState: AsyncState<Unit> = AsyncState.Idle,
        val isShareDialogOpen: Boolean = false,
        val username: String = "",
    )

    @Immutable
    data class ViewState(
        val friends: List<Friend>,
        val addFriendNick: String,
        val deleteFriendDialogState: DeleteFriendDialogState,
        val selectedFriend: Friend?,
        val actionState: AsyncState<Unit>,
        val isShareDialogOpen: Boolean,
        val username: String,
    )

    sealed class DeleteFriendDialogState {
        data class Open(val friend: Friend): DeleteFriendDialogState()
        data object Closed: DeleteFriendDialogState()
    }

    companion object {
        private const val TAG = "FRIENDS_VM"
    }
}
