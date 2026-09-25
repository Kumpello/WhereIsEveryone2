package com.kumpello.whereiseveryone.main.friends.presentation

import androidx.annotation.StringRes
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.common.extension.isAddFriendDeepLink
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class AddFriendViewModel(
    private val addFriendUseCase: AddFriendUseCase
) : BaseViewModel<AddFriendViewModel.State, AddFriendViewModel.ViewState, AddFriendViewModel.Event, AddFriendViewModel.Action>(
    State()
) {

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.SetAddFriendNick -> state.copy(addFriendNick = event.nick).toResult()
            Event.AddFriend, is Event.ConfirmLinkedFriend -> {
                if (state.actionState.isLoading) return state.toResult()
                val username = when (event) {
                    is Event.ConfirmLinkedFriend -> {
                        if (state.pendingLinkedFriend != event.username) return state.toResult()
                        event.username
                    }
                    else -> {
                        if (state.pendingLinkedFriend != null) return state.toResult()
                        state.addFriendNick
                    }
                }
                Timber.tag(TAG).d("Adding friend")
                state.copy(
                    addFriendNick = username,
                    pendingLinkedFriend = null,
                    actionState = AsyncState.Loading(message = "Adding friend...")
                ).toResult(SideEffect.AsyncWork {
                    try {
                        Timber.tag(TAG).d("Executing add-friend request")
                        when (val response = addFriendUseCase.execute(username)) {
                            CodeResponse.SuccessNoContent -> {
                                Timber.tag(TAG).d("AddFriend: Success")
                                Event.OnActionSuccess(R.string.friend_added)
                            }

                            is CodeResponse.ErrorData -> {
                                Timber.tag(TAG).d("Add-friend request rejected")
                                Event.OnError(R.string.error_adding_friend)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Unable to add friend")
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
                if (!event.uri.isAddFriendDeepLink() || state.actionState.isLoading || state.pendingLinkedFriend != null) {
                    return state.toResult()
                }
                state.copy(pendingLinkedFriend = event.uri.lastPathSegment).toResult()
            }

            Event.DismissLinkedFriend -> state.copy(pendingLinkedFriend = null).toResult()
            Event.ScanQrCode -> state.toResult(SideEffect.Effect(Action.OpenQrScanner))
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            addFriendNick = addFriendNick,
            actionState = actionState,
            pendingLinkedFriend = pendingLinkedFriend
        )
    }

    sealed class Action {
        data class Toast(@StringRes val id: Int) : Action()
        data object NotifyFriendAdded : Action()
        data object OpenQrScanner : Action()
    }

    sealed class Event {
        data class SetAddFriendNick(val nick: String) : Event()
        data object AddFriend : Event()
        data class OnActionSuccess(@StringRes val messageId: Int) : Event()
        data class OnError(@StringRes val id: Int) : Event()
        data class OnUriReceived(val uri: android.net.Uri) : Event()
        data class ConfirmLinkedFriend(val username: String) : Event()
        data object DismissLinkedFriend : Event()
        data object ScanQrCode : Event()
    }

    data class State(
        val addFriendNick: String = "",
        val pendingLinkedFriend: String? = null,
        val actionState: AsyncState<Unit> = AsyncState.Idle
    )

    @Immutable
    data class ViewState(
        val addFriendNick: String,
        val actionState: AsyncState<Unit>,
        val pendingLinkedFriend: String? = null
    )

    companion object {
        private const val TAG = "ADD_FRIEND_VM"
    }
}
