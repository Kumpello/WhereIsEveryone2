package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.common.entities.ScreenState
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

) : ViewModel() {
    private var _state = MutableStateFlow(State())
    val state: StateFlow<ViewState> = _state.map { state ->
        state.toViewState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _state.value.toViewState()
    )

    private val _action = MutableSharedFlow<LoginViewModel.Action>()
    val action: SharedFlow<LoginViewModel.Action> = _action.asSharedFlow()

    fun trigger(command: Command) {
        when (command) {
            is Command.SetAddFriendNick -> setAddFriendNick(command.nick)
        }
    }

    private fun setAddFriendNick(nick : String) {
        _state.value = _state.value.copy(
            addFriendNick = nick
        )
    }

    private fun State.toViewState(): ViewState {
        return ViewState(
            screenState = screenState,
            friends = friends,
            addFriendNick = addFriendNick
        )
    }

    sealed class Action {

    }

    sealed class Command {
        data class SetAddFriendNick(val nick: String) : Command()
    }

    data class State(
        val screenState: ScreenState = ScreenState.Success,
        val friends: List<Friend> = emptyList(),
        val addFriendNick: String = ""
    )

    data class ViewState(
        val screenState: ScreenState,
        val friends: List<Friend>, //TODO: To be changed (?)
        val addFriendNick: String
    )
}