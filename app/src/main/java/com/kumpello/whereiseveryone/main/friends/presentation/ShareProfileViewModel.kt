package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import kotlinx.coroutines.launch

class ShareProfileViewModel(
    private val getKeyUseCase: GetKeyUseCase
) : BaseViewModel<ShareProfileViewModel.State, ShareProfileViewModel.ViewState, ShareProfileViewModel.Event, ShareProfileViewModel.Action>(
    State()
) {

    init {
        viewModelScope.launch {
            val username = getKeyUseCase.getValue(WhereIsEveryoneApplication.USER_NAME_KEY)
            trigger(Event.OnUsernameLoaded(username ?: ""))
        }
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.OnUsernameLoaded -> state.copy(username = event.username).toResult()
            Event.OnNfcNotSupported -> state.toResult(SideEffect.Effect(Action.Toast(R.string.nfc_not_supported)))
            Event.OnNfcDisabled -> state.toResult(SideEffect.Effect(Action.Toast(R.string.nfc_disabled)))
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            username = username
        )
    }

    sealed class Action {
        data class Toast(@StringRes val id: Int) : Action()
    }

    sealed class Event {
        data class OnUsernameLoaded(val username: String) : Event()
        data object OnNfcNotSupported : Event()
        data object OnNfcDisabled : Event()
    }

    data class State(
        val username: String = ""
    )

    @Immutable
    data class ViewState(
        val username: String
    )
}
