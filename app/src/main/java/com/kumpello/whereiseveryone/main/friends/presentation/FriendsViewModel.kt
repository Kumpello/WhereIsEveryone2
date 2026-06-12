package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.R
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
            is Command.SelectFriend -> selectFriend(command.friend)
            Command.ClearSelectedFriend -> clearSelectedFriend()
        }
    }

    fun checkFriends() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
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
                        state.update { state ->
                            state.copy(
                                friends = friendList
                            )
                        }
                    }
                    is FriendsResponse.ErrorData -> {
                        _action.emit(Action.Toast(R.string.error_getting_friends))
                        Timber.d("Error getting friends!\n%s", friends)
                    }
                }
            }.onFailure { error ->
                _action.emit(Action.Toast(R.string.error_getting_friends))
                Timber.d("Error getting friends!\n%s", error.message.toString())
            }
        }
    }

    private fun addFriend() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val response = addFriendUseCase.execute(state.value.addFriendNick)) {
                    CodeResponse.SuccessNoContent -> {
                        _action.emit(Action.Toast(R.string.friend_added))
                        checkFriends()
                    }
                    is CodeResponse.ErrorData -> {
                        Timber.e(response.toString())
                        _action.emit(Action.Toast(R.string.error_adding_friend))
                    }
                }
            }.onFailure { error ->
                _action.emit(Action.Toast(R.string.error_adding_friend))
                Timber.e("Error adding friend!\n%s", error.toString())
            }
        }
    }

    private fun deleteFriend(nick: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val response = removeFriendUseCase.execute(nick)) {
                    CodeResponse.SuccessNoContent -> {
                        _action.emit(Action.Toast(R.string.friend_deleted_successfully))
                        checkFriends()
                    }
                    is CodeResponse.ErrorData -> {
                        _action.emit(Action.Toast(R.string.error_deleting_friend))
                    }
                }
            }.onFailure { error ->
                _action.emit(Action.Toast(R.string.error_deleting_friend))
                Timber.d("Error deleting friend!\n%s", error.message.toString())
            }
        }
    }

    private fun acceptFriend(nick: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val response = acceptFriendUseCase.execute(nick)) {
                    CodeResponse.SuccessNoContent -> {
                        _action.emit(Action.Toast(R.string.friend_accepted))
                        checkFriends()
                    }
                    is CodeResponse.ErrorData -> {
                        _action.emit(Action.Toast(R.string.error_occurred_during_accepting_friend))
                    }
                }
            }.onFailure { error ->
                _action.emit(Action.Toast(R.string.error_occurred_during_accepting_friend))
                Timber.d("Error accepting friend!\n%s", error.message.toString())
            }
        }
    }

    private fun rejectFriend(nick: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val response = rejectFriendUseCase.execute(nick)) {
                    CodeResponse.SuccessNoContent -> {
                        _action.emit(Action.Toast(R.string.rejected_successfully))
                        checkFriends()
                    }
                    is CodeResponse.ErrorData -> {
                        _action.emit(Action.Toast(R.string.error_during_reject))
                    }
                }
            }.onFailure { error ->
                _action.emit(Action.Toast(R.string.error_during_reject))
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

    private fun selectFriend(friend: Friend) {
        state.update {
            it.copy(
                selectedFriend = friend
            )
        }
    }

    private fun clearSelectedFriend() {
        state.update {
            it.copy(
                selectedFriend = null
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
            selectedFriend = selectedFriend
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
        data class SetAddFriendNick(val nick: String) : Command()
        data object AddFriend : Command()
        data class DeleteFriend(val nick: String): Command()
        data class AcceptFriend(val nick: String): Command()
        data class RejectFriend(val nick: String): Command()
        data class OpenDeleteFriendDialog(val friend: Friend): Command()
        data object CloseDeleteFriendDialog: Command()
        data class SelectFriend(val friend: Friend): Command()
        data object ClearSelectedFriend: Command()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val friends: List<FriendLocalData> = emptyList(),
        val addFriendNick: String = "",
        val deleteFriendDialogState: DeleteFriendDialogState = DeleteFriendDialogState.Closed,
        val selectedFriend: Friend? = null,
        val userLocation: LocationData = LocationData(
            lat = 0.0,
            lon = 0.0,
            bearing = 0.0f,
            alt = 0.0,
            accuracy = 0.0f,
            last_update = Instant.DISTANT_PAST
        ),
    )

    data class ViewState(
        //val screenState: ScreenState, //TODO: Consider adding loading state
        val friends: List<Friend>,
        val addFriendNick: String,
        val deleteFriendDialogState: DeleteFriendDialogState,
        val selectedFriend: Friend?
    )

    sealed class DeleteFriendDialogState {
        data class Open(val friend: Friend): DeleteFriendDialogState()
        data object Closed: DeleteFriendDialogState()
    }
}