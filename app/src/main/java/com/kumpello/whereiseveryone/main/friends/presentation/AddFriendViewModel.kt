package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.annotation.StringRes
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import androidx.compose.runtime.Immutable
import timber.log.Timber

class AddFriendViewModel(
    private val addFriendUseCase: AddFriendUseCase
) : BaseViewModel<AddFriendViewModel.State, AddFriendViewModel.ViewState, AddFriendViewModel.Event, AddFriendViewModel.Action>(
    State()
) {

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
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

            is Event.OnActionSuccess -> state.copy(actionState = AsyncState.Idle, addFriendNick = "").toResult(
                SideEffect.Effect(Action.Toast(event.messageId)),
                SideEffect.Effect(Action.NotifyFriendAdded)
            )

            is Event.OnError -> state.copy(actionState = AsyncState.Idle).toResult(SideEffect.Effect(Action.Toast(event.id)))
            
            is Event.OnUriReceived -> {
                val username = event.uri.lastPathSegment
                if (username != null) {
                    state.copy(addFriendNick = username).toResult(SideEffect.InternalEvent(Event.AddFriend))
                } else {
                    state.toResult()
                }
            }
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            addFriendNick = addFriendNick,
            actionState = actionState
        )
    }

    sealed class Action {
        data class Toast(@StringRes val id: Int) : Action()
        data object NotifyFriendAdded : Action()
    }

    sealed class Event {
        data class SetAddFriendNick(val nick: String) : Event()
        data object AddFriend : Event()
        data class OnActionSuccess(@StringRes val messageId: Int) : Event()
        data class OnError(@StringRes val id: Int) : Event()
        data class OnUriReceived(val uri: android.net.Uri) : Event()
    }

    data class State(
        val addFriendNick: String = "",
        val actionState: AsyncState<Unit> = AsyncState.Idle
    )

    @Immutable
    data class ViewState(
        val addFriendNick: String,
        val actionState: AsyncState<Unit>
    )

    companion object {
        private const val TAG = "ADD_FRIEND_VM"
    }
}
