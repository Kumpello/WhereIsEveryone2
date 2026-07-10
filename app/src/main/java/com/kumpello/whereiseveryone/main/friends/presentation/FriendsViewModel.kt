package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapFriendUseCase
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendLocalData
import com.kumpello.whereiseveryone.main.common.entity.LocationData
import com.kumpello.whereiseveryone.main.common.entity.toFriendState
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AcceptFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.GetPausedFriendsUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RejectFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.ResumeSharingUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.StopSharingUseCase
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Clock
import kotlin.time.Instant

class FriendsViewModel(
    private val removeFriendUseCase: RemoveFriendUseCase,
    private val getFriendsDataUseCase: GetFriendsDataUseCase,
    private val acceptFriendUseCase: AcceptFriendUseCase,
    private val rejectFriendUseCase: RejectFriendUseCase,
    private val locationService: LocationService,
    private val mapFriendUseCase: MapFriendUseCase,
    private val stopSharingUseCase: StopSharingUseCase,
    private val resumeSharingUseCase: ResumeSharingUseCase,
    private val getPausedFriendsUseCase: GetPausedFriendsUseCase,
    private val getKeyUseCase: GetKeyUseCase
) : BaseViewModel<FriendsViewModel.State, FriendsViewModel.ViewState, FriendsViewModel.Event, FriendsViewModel.Action>(
    State()
) {

    init {
        viewModelScope.launch {
            val username = getKeyUseCase.getValue(WhereIsEveryoneApplication.USER_NAME_KEY)
            trigger(Event.OnUsernameLoaded(username ?: ""))
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
        trigger(Event.CheckFriends)
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.OnLocationUpdate -> state.copy(userLocation = event.location).toResult()
            Event.CheckFriends -> {
                Timber.tag(TAG).d("Checking friends")
                state.toResult(SideEffect.AsyncWork {
                    try {
                        val friendsResponse = getFriendsDataUseCase.execute()
                        val pausedResponse = getPausedFriendsUseCase.execute()

                        val paused = if (pausedResponse is SharingResponse.PausedFriends) {
                            pausedResponse.usernames
                        } else {
                            Timber.tag(TAG).d("Error getting paused friends!\n%s", pausedResponse)
                            emptyList()
                        }

                        when (friendsResponse) {
                            is FriendsResponse.FriendsData -> {
                                val friendList = friendsResponse.positions.map { friendData ->
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
                                                last_update = Instant.parse(loc.last_update)
                                            )
                                        },
                                        friendSince = friendData.friend_since?.let { Instant.parse(it) }
                                    )
                                }
                                Event.OnFriendsLoaded(friendList, paused)
                            }

                            is FriendsResponse.ErrorData -> {
                                Timber.tag(TAG).d("Error getting friends!\n%s", friendsResponse)
                                Event.OnError(R.string.error_getting_friends)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Error getting friends!\n%s", e.message.toString())
                        Event.OnError(R.string.error_getting_friends)
                    }
                })
            }

            is Event.OnFriendsLoaded -> state.copy(
                friends = event.friends,
                pausedFriends = event.pausedFriends
            ).toResult()

            is Event.OnUsernameLoaded -> state.copy(username = event.username).toResult()

            is Event.OnError -> state.copy(actionState = AsyncState.Idle)
                .toResult(SideEffect.Effect(Action.Toast(event.id)))

            is Event.DeleteFriend -> {
                Timber.tag(TAG).d("Deleting friend: %s", event.nick)
                state.copy(actionState = AsyncState.Loading(message = "Deleting friend..."))
                    .toResult(SideEffect.AsyncWork {
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
                state.copy(actionState = AsyncState.Loading(message = "Accepting request..."))
                    .toResult(SideEffect.AsyncWork {
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
                state.copy(actionState = AsyncState.Loading(message = "Rejecting request..."))
                    .toResult(SideEffect.AsyncWork {
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

            is Event.ToggleSharing -> {
                val isPaused = state.pausedFriends.contains(event.nick)
                val (useCase, loadingMsg, successMsg, errorMsg) = if (isPaused) {
                    listOf(
                        resumeSharingUseCase::execute,
                        "Resuming sharing...",
                        R.string.sharing_resumed_successfully,
                        R.string.error_resuming_sharing
                    )
                } else {
                    listOf(
                        stopSharingUseCase::execute,
                        "Stopping sharing...",
                        R.string.sharing_stopped_successfully,
                        R.string.error_stopping_sharing
                    )
                }
                
                state.copy(actionState = AsyncState.Loading(message = loadingMsg as String))
                    .toResult(SideEffect.AsyncWork {
                        try {
                            val response = (useCase as suspend (String) -> CodeResponse).invoke(event.nick)
                            if (response is CodeResponse.SuccessNoContent) {
                                Event.OnActionSuccess(successMsg as Int)
                            } else {
                                Event.OnError(errorMsg as Int)
                            }
                        } catch (e: Exception) {
                            Timber.tag(TAG).d("Error toggling sharing!\n%s", e.message.toString())
                            Event.OnError(errorMsg as Int)
                        }
                    })
            }

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
        }
    }

    override fun State.toViewState(): ViewState {
        val mappedFriends = friends.map { friend ->
            mapFriendUseCase.execute(friend, userLocation).copy(
                isPaused = pausedFriends.contains(friend.username)
            )
        }.sortedBy { it.distance ?: Double.MAX_VALUE }

        return ViewState(
            friends = mappedFriends,
            deleteFriendDialogState = deleteFriendDialogState,
            selectedFriend = selectedFriend,
            actionState = actionState,
            isShareDialogOpen = isShareDialogOpen,
            username = username,
            friendUsername = friendUsername
        )
    }

    sealed class Action {
        data class Toast(@StringRes val id: Int) : Action()
        data object BackToMap : Action()
    }

    sealed class Event {
        data class OnLocationUpdate(val location: LocationData?) : Event()
        data object CheckFriends : Event()
        data class OnFriendsLoaded(
            val friends: List<FriendLocalData>,
            val pausedFriends: List<String>
        ) : Event()
        data class OnUsernameLoaded(val username: String) : Event()
        data class OnError(@StringRes val id: Int) : Event()
        data class DeleteFriend(val nick: String) : Event()
        data class AcceptFriend(val nick: String) : Event()
        data class RejectFriend(val nick: String) : Event()
        data class ToggleSharing(val nick: String) : Event()
        data class OpenDeleteFriendDialog(val friend: Friend) : Event()
        data object CloseDeleteFriendDialog : Event()
        data class SelectFriend(val friend: Friend) : Event()
        data object ClearSelectedFriend : Event()
        data class OnActionSuccess(@StringRes val messageId: Int) : Event()
        data object OpenShareDialog : Event()
        data object CloseShareDialog : Event()
    }

    data class State(
        val friends: List<FriendLocalData> = emptyList(),
        val pausedFriends: List<String> = emptyList(),
        val deleteFriendDialogState: DeleteFriendDialogState = DeleteFriendDialogState.Closed,
        val selectedFriend: Friend? = null,
        val userLocation: LocationData? = null,
        val actionState: AsyncState<Unit> = AsyncState.Idle,
        val isShareDialogOpen: Boolean = false,
        val username: String = "",
        val friendUsername: String = ""
    )

    @Immutable
    data class ViewState(
        val friends: List<Friend>,
        val deleteFriendDialogState: DeleteFriendDialogState,
        val selectedFriend: Friend?,
        val actionState: AsyncState<Unit>,
        val isShareDialogOpen: Boolean,
        val username: String,
        val friendUsername: String
    )

    sealed class DeleteFriendDialogState {
        data class Open(val friend: Friend) : DeleteFriendDialogState()
        data object Closed : DeleteFriendDialogState()
    }

    companion object {
        private const val TAG = "FRIENDS_VM"
    }
}
