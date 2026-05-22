package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.entity.ScreenState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Clock
import kotlin.time.Instant

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
) : ViewModel() {
    private var state = MutableStateFlow(State())
    val viewState: StateFlow<ViewState> = state.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = state.value.toViewState()
    )

    init {
        checkFriends()
        viewModelScope.launch(Dispatchers.IO) {
            locationService.observeLocation().collect { location ->
                state.update {
                    it.copy(
                        userLocation = LocationData(
                            lat = location.latitude,
                            lon = location.longitude,
                            bearing = location.bearing,
                            alt = location.altitude,
                            accuracy = location.accuracy,
                            last_update = Clock.System.now()
                        )
                    )
                }
            }
        }
    }

    private val _action = MutableSharedFlow<Action>()
    val action: SharedFlow<Action> = _action.asSharedFlow()

    fun trigger(command: Command) {
        when (command) {
            is Command.SetAddFriendNick -> setAddFriendNick(command.nick)
            Command.AddFriend -> addFriend()
            is Command.DeleteFriend -> deleteFriend(command.nick)
            is Command.OpenDeleteFriendDialog -> openDeleteFriendDialog(command.friend)
            Command.CloseDeleteFriendDialog -> closeDeleteFriendDialog()
            is Command.AcceptFriend -> acceptFriend(command.nick)
            is Command.RejectFriend -> rejectFriend(command.nick)
        }
    }

    private fun addFriend() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val response = addFriendUseCase.execute(state.value.addFriendNick)) {
                    CodeResponse.SuccessNoContent -> {
                        //TODO: Toast!
                        checkFriends()
                    }
                    is CodeResponse.ErrorData -> {
                        Timber.e(response.toString())
                        //TODO: Toast!
                    }
                }
            }.onFailure { error ->
                //TODO: Toast!
                Timber.e("Error adding friend!\n%s", error.toString())
            }
        }
    }

    private fun deleteFriend(nick: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val response = removeFriendUseCase.execute(nick)) {
                    CodeResponse.SuccessNoContent -> {
                        //TODO: Toast!
                        checkFriends()
                    }
                    is CodeResponse.ErrorData -> {
                        //TODO: Toast!
                    }
                }
            }.onFailure { error ->
                //TODO: Toast!
                Timber.d("Error deleting friend!\n%s", error.message.toString())
            }
        }
    }

    private fun acceptFriend(nick: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val response = acceptFriendUseCase.execute(nick)) {
                    CodeResponse.SuccessNoContent -> {
                        //TODO: Toast!
                        checkFriends()
                    }
                    is CodeResponse.ErrorData -> {
                        //TODO: Toast!
                    }
                }
            }.onFailure { error ->
                //TODO: Toast!
                Timber.d("Error accepting friend!\n%s", error.message.toString())
            }
        }
    }

    private fun rejectFriend(nick: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val response = rejectFriendUseCase.execute(nick)) {
                    CodeResponse.SuccessNoContent -> {
                        //TODO: Toast!
                        checkFriends()
                    }
                    is CodeResponse.ErrorData -> {
                        //TODO: Toast!
                    }
                }
            }.onFailure { error ->
                //TODO: Toast!
                Timber.d("Error rejecting friend!\n%s", error.message.toString())
            }
        }
    }

    private fun openDeleteFriendDialog(friend: Friend) {
        state.update {
            it.copy(
                deleteFriendDialogState = DeleteFriendDialogState.Open(friend)
            )
        }
    }

    private fun closeDeleteFriendDialog() {
        state.update {
            it.copy(
                deleteFriendDialogState = DeleteFriendDialogState.Closed
            )
        }
    }

    private fun setAddFriendNick(nick : String) {
        state.update {
            it.copy(
                addFriendNick = nick
            )
        }
    }

    private fun checkFriends() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val friends = getFriendsDataUseCase.execute()) { //TODO: emit action
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
                        state.update { state ->
                            state.copy(
                                friends = friendList
                            )
                        }
                    }
                    is FriendsResponse.ErrorData -> {
                        //TODO: Toast!
                        Timber.d("Error getting friends!\n%s", friends)
                    }
                }
            }.onFailure { error ->
                //TODO: Toast!
                Timber.d("Error getting friends!\n%s", error.message.toString())
            }
        }
    }

    private fun State.toViewState(): ViewState {
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
                        alt = convertAltUseCase.execute(userLocation.alt, friend.location.alt),
                        accuracy = convertAccuracyUseCase.execute(friend.location.accuracy),
                        lastUpdateTime = friend.location.last_update.toString(),
                        lastUpdateAge = convertLastUpdateUseCase.execute(friend.location.last_update)
                    )
                )
            },
            addFriendNick = addFriendNick,
            deleteFriendDialogState = deleteFriendDialogState
        )
    }

    sealed class Action {
        data class AddFriendResult(val success: Boolean) : Action()
        data class DeleteFriendResult(val success: Boolean) : Action()
        data object BackToMap : Action()
    }

    sealed class Command {
        data class SetAddFriendNick(val nick: String) : Command()
        data object AddFriend : Command()
        data class DeleteFriend(val nick: String): Command()
        data class AcceptFriend(val nick: String): Command()
        data class RejectFriend(val nick: String): Command()
        data class OpenDeleteFriendDialog(val friend: Friend): Command()
        data object CloseDeleteFriendDialog: Command()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val friends: List<FriendLocalData> = emptyList(),
        val addFriendNick: String = "",
        val deleteFriendDialogState: DeleteFriendDialogState = DeleteFriendDialogState.Closed,
        val userLocation: LocationData = LocationData(
            lat = 0.0,
            lon = 0.0,
            bearing = 0.0f,
            alt = 0.0,
            accuracy = 0.0f,
            last_update = Instant.DISTANT_PAST
        )
    )

    data class ViewState(
        //val screenState: ScreenState, //TODO: Delete?
        val friends: List<Friend>,
        val addFriendNick: String,
        val deleteFriendDialogState: DeleteFriendDialogState
    )

    sealed class DeleteFriendDialogState {
        data class Open(val friend: Friend): DeleteFriendDialogState()
        data object Closed: DeleteFriendDialogState()
    }
}