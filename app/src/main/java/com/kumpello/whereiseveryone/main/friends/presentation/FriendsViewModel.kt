package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
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
    private val locationService: LocationService
) : BaseViewModel<FriendsViewModel.State, FriendsViewModel.ViewState, FriendsViewModel.Command, FriendsViewModel.Action>(
    State()
) {

    init {
        viewModelScope.launch {
            locationService.observeLocation().collect { location ->
                trigger(Command.OnLocationUpdate(location?.let {
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
        trigger(Command.CheckFriends)
    }

    override fun reduce(state: State, event: Command): ReducerResult<State, Command, Action> {
        return when (event) {
            is Command.OnLocationUpdate -> state.copy(userLocation = event.location).toResult()
            is Command.CheckFriends -> {
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
                                Command.OnFriendsLoaded(friendList)
                            }

                            is FriendsResponse.ErrorData -> {
                                Timber.tag(TAG).d("Error getting friends!\n%s", friends)
                                Command.OnError(R.string.error_getting_friends)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Error getting friends!\n%s", e.message.toString())
                        Command.OnError(R.string.error_getting_friends)
                    }
                })
            }

            is Command.OnFriendsLoaded -> state.copy(friends = event.friends).toResult()
            is Command.OnError -> state.copy(actionState = AsyncState.Idle).toResult(SideEffect.Effect(Action.Toast(event.id)))

            is Command.SetAddFriendNick -> state.copy(addFriendNick = event.nick).toResult()

            Command.AddFriend -> {
                Timber.tag(TAG).d("Adding friend: %s", state.addFriendNick)
                state.copy(actionState = AsyncState.Loading(message = "Adding friend...")).toResult(SideEffect.AsyncWork {
                    try {
                        when (val response = addFriendUseCase.execute(state.addFriendNick)) {
                            CodeResponse.SuccessNoContent -> {
                                Command.OnActionSuccess(R.string.friend_added)
                            }

                            is CodeResponse.ErrorData -> {
                                Timber.tag(TAG).e(response.toString())
                                Command.OnError(R.string.error_adding_friend)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e("Error adding friend!\n%s", e.toString())
                        Command.OnError(R.string.error_adding_friend)
                    }
                })
            }

            is Command.DeleteFriend -> {
                Timber.tag(TAG).d("Deleting friend: %s", event.nick)
                state.copy(actionState = AsyncState.Loading(message = "Deleting friend...")).toResult(SideEffect.AsyncWork {
                    try {
                        when (val response = removeFriendUseCase.execute(event.nick)) {
                            CodeResponse.SuccessNoContent -> {
                                Command.OnActionSuccess(R.string.friend_deleted_successfully)
                            }

                            is CodeResponse.ErrorData -> {
                                Command.OnError(R.string.error_deleting_friend)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Error deleting friend!\n%s", e.message.toString())
                        Command.OnError(R.string.error_deleting_friend)
                    }
                })
            }

            is Command.AcceptFriend -> {
                Timber.tag(TAG).d("Accepting friend: %s", event.nick)
                state.copy(actionState = AsyncState.Loading(message = "Accepting request...")).toResult(SideEffect.AsyncWork {
                    try {
                        when (val response = acceptFriendUseCase.execute(event.nick)) {
                            CodeResponse.SuccessNoContent -> {
                                Command.OnActionSuccess(R.string.friend_accepted)
                            }

                            is CodeResponse.ErrorData -> {
                                Command.OnError(R.string.error_occurred_during_accepting_friend)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Error accepting friend!\n%s", e.message.toString())
                        Command.OnError(R.string.error_occurred_during_accepting_friend)
                    }
                })
            }

            is Command.RejectFriend -> {
                Timber.tag(TAG).d("Rejecting friend: %s", event.nick)
                state.copy(actionState = AsyncState.Loading(message = "Rejecting request...")).toResult(SideEffect.AsyncWork {
                    try {
                        when (val response = rejectFriendUseCase.execute(event.nick)) {
                            CodeResponse.SuccessNoContent -> {
                                Command.OnActionSuccess(R.string.rejected_successfully)
                            }

                            is CodeResponse.ErrorData -> {
                                Command.OnError(R.string.error_during_reject)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Error rejecting friend!\n%s", e.message.toString())
                        Command.OnError(R.string.error_during_reject)
                    }
                })
            }

            is Command.OnActionSuccess -> state.copy(actionState = AsyncState.Idle).toResult(
                SideEffect.Effect(Action.Toast(event.messageId)),
                SideEffect.InternalEvent(Command.CheckFriends)
            )

            is Command.OpenDeleteFriendDialog -> state.copy(
                deleteFriendDialogState = DeleteFriendDialogState.Open(event.friend)
            ).toResult()

            Command.CloseDeleteFriendDialog -> state.copy(
                deleteFriendDialogState = DeleteFriendDialogState.Closed
            ).toResult()

            is Command.SelectFriend -> state.copy(selectedFriend = event.friend).toResult()
            Command.ClearSelectedFriend -> state.copy(selectedFriend = null).toResult()
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            friends = friends.map { friend ->
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
                    )
                )
            },
            addFriendNick = addFriendNick,
            deleteFriendDialogState = deleteFriendDialogState,
            selectedFriend = selectedFriend,
            actionState = actionState
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
    }

    sealed class Command {
        data class OnLocationUpdate(val location: LocationData?) : Command()
        data object CheckFriends : Command()
        data class OnFriendsLoaded(val friends: List<FriendLocalData>) : Command()
        data class OnError(@StringRes val id: Int) : Command()
        data class SetAddFriendNick(val nick: String) : Command()
        data object AddFriend : Command()
        data class DeleteFriend(val nick: String): Command()
        data class AcceptFriend(val nick: String): Command()
        data class RejectFriend(val nick: String): Command()
        data class OpenDeleteFriendDialog(val friend: Friend): Command()
        data object CloseDeleteFriendDialog: Command()
        data class SelectFriend(val friend: Friend): Command()
        data object ClearSelectedFriend: Command()
        data class OnActionSuccess(@StringRes val messageId: Int) : Command()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val friends: List<FriendLocalData> = emptyList(),
        val addFriendNick: String = "",
        val deleteFriendDialogState: DeleteFriendDialogState = DeleteFriendDialogState.Closed,
        val selectedFriend: Friend? = null,
        val userLocation: LocationData? = null,
        val actionState: AsyncState<Unit> = AsyncState.Idle
    )

    @Immutable
    data class ViewState(
        val friends: List<Friend>,
        val addFriendNick: String,
        val deleteFriendDialogState: DeleteFriendDialogState,
        val selectedFriend: Friend?,
        val actionState: AsyncState<Unit>
    )

    sealed class DeleteFriendDialogState {
        data class Open(val friend: Friend): DeleteFriendDialogState()
        data object Closed: DeleteFriendDialogState()
    }

    companion object {
        private const val TAG = "FRIENDS_VM"
    }
}
