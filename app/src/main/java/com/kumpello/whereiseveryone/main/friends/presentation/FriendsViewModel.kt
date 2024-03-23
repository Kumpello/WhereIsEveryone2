package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.main.common.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.friends.model.Friend
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FriendsViewModel(
    private val addFriendUseCase: AddFriendUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase
) : ViewModel() {
    private var _state = MutableStateFlow(State())
    val state: StateFlow<ViewState> = _state.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _state.value.toViewState()
    )

    private val _action = MutableSharedFlow<Action>()
    val action: SharedFlow<Action> = _action.asSharedFlow()

    fun trigger(command: Command) {
        when (command) {
            is Command.SetAddFriendNick -> setAddFriendNick(command.nick)
            Command.AddFriend -> addFriend()
            is Command.DeleteFriend -> deleteFriend(command.id)
            is Command.OpenDeleteFriendDialog -> openDeleteFriendDialog(command.friend)
            Command.CloseDeleteFriendDialog -> closeDeleteFriendDialog()
        }
    }

    private fun addFriend() {
        viewModelScope.runCatching {
            addFriendUseCase.execute(_state.value.addFriendNick)
        }
    }

    private fun deleteFriend(id: String) {
        viewModelScope.runCatching {
            removeFriendUseCase.execute(id)
        }
    }

    private fun openDeleteFriendDialog(friend: Friend) {
        _state.value = _state.value.copy(
            deleteFriendDialogState = DeleteFriendDialogState.Open(friend)
        )
    }

    private fun closeDeleteFriendDialog() {
        _state.value = _state.value.copy(
            deleteFriendDialogState = DeleteFriendDialogState.Closed
        )
    }

    private fun setAddFriendNick(nick : String) {
        _state.value = _state.value.copy(
            addFriendNick = nick
        )
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
        data class OpenDeleteFriendDialog(val friend: Friend) : Action()
        data object CloseDeleteFriendDialog : Action()
        data class AddFriendResult(val success: Boolean) : Action()
        data class DeleteFriendResult(val success: Boolean) : Action()
    }

    sealed class Command {
        data class SetAddFriendNick(val nick: String) : Command()
        data object AddFriend : Command()
        data class OpenDeleteFriendDialog(val friend: Friend): Command()
        data object CloseDeleteFriendDialog: Command()
        data class DeleteFriend(val id: String): Command()
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