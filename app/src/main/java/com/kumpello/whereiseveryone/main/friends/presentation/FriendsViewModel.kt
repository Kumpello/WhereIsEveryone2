package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
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

class FriendsViewModel(
    private val addFriendUseCase: AddFriendUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase,
    private val getFriendsDataUseCase: GetFriendsDataUseCase
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
        when (val friends = getFriendsDataUseCase.execute()) {
            is PositionsResponse.FriendsData -> {
                val friendList = friends.positions.map { friendData ->
                    friendData.friend
                }
                state.update { state ->
                    state.copy(
                        friends = friendList
                    )
                }
            }
            else -> {
                //TODO: Toast
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
        }
    }

    private fun addFriend() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                addFriendUseCase.execute(state.value.addFriendNick) //TODO: Get result, emit action
            }
        }
    }

    private fun deleteFriend(nick: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                removeFriendUseCase.execute(nick) //TODO: Get result, emit action
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

    private fun State.toViewState(): ViewState {
        return ViewState(
            //screenState = screenState,
            friends = friends,
            addFriendNick = addFriendNick,
            deleteFriendDialogState = deleteFriendDialogState
        )
    }

    sealed class Action {
        data class AddFriendResult(val success: Boolean) : Action()
        data class DeleteFriendResult(val success: Boolean) : Action()
    }

    sealed class Command {
        data class SetAddFriendNick(val nick: String) : Command()
        data object AddFriend : Command()
        data class OpenDeleteFriendDialog(val friend: Friend): Command()
        data object CloseDeleteFriendDialog: Command()
        data class DeleteFriend(val nick: String): Command()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Map,
        val friends: List<Friend> = emptyList(),
        val addFriendNick: String = "",
        val deleteFriendDialogState: DeleteFriendDialogState = DeleteFriendDialogState.Closed
    )

    data class ViewState(
        //val screenState: ScreenState, //TODO: Delete?
        val friends: List<Friend>, //TODO: To be changed (?)
        val addFriendNick: String,
        val deleteFriendDialogState: DeleteFriendDialogState
    )

    sealed class DeleteFriendDialogState {
        data class Open(val friend: Friend): DeleteFriendDialogState()
        data object Closed: DeleteFriendDialogState()
    }
}