package com.kumpello.whereiseveryone.main.map.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.common.presentation.BaseViewModel
import com.kumpello.whereiseveryone.main.map.domain.usecase.UpdateStatusUseCase
import timber.log.Timber

class MessageViewModel(
    private val saveKeyUseCase: SaveKeyUseCase,
    private val getKeyUseCase: GetKeyUseCase,
    private val updateStatusUseCase: UpdateStatusUseCase,
) : BaseViewModel<MessageViewModel.State, MessageViewModel.ViewState, MessageViewModel.Event, MessageViewModel.Action>(
    State()
) {

    init {
        trigger(Event.LoadUserMessage)
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            Event.LoadUserMessage -> state.toResult(SideEffect.AsyncWork {
                val message = getKeyUseCase.getValue(WhereIsEveryoneApplication.USER_MESSAGE_KEY).orEmpty()
                Timber.tag(TAG).d("Loaded user message: %s", message)
                Event.OnUserMessageLoaded(message)
            })

            is Event.OnUserMessageLoaded -> state.copy(userMessage = event.message).toResult()

            is Event.WriteMessage -> state.copy(userMessageField = event.message).toResult()

            Event.SendMessage -> state.toResult(SideEffect.AsyncWork {
                try {
                    val message = state.userMessageField
                    when (updateStatusUseCase.execute(message)) {
                        is CodeResponse.SuccessNoContent -> {
                            saveKeyUseCase.saveValue(WhereIsEveryoneApplication.USER_MESSAGE_KEY, message)
                            Timber.tag(TAG).d("Message saved to DataStore")
                            Event.OnMessageSent(message)
                        }

                        is CodeResponse.ErrorData -> {
                            Timber.tag(TAG).d("Error updating message!")
                            Event.OnMessageError(R.string.error_updating_message)
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).d("Error updating message!\n%s", e.message.toString())
                    Event.OnMessageError(R.string.error_updating_message)
                }
            })

            is Event.OnMessageSent -> {
                Timber.tag(TAG).d("Message updated successfully: %s", event.message)
                state.copy(userMessage = event.message, userMessageField = "").toResult(
                    SideEffect.Effect(Action.NotifyMessageSent)
                )
            }

            is Event.OnMessageError -> state.toResult(SideEffect.Effect(Action.Toast(event.errorId)))

            Event.ClearMessage -> state.copy(userMessageField = "").toResult(SideEffect.InternalEvent(Event.SendMessage))
        }
    }

    override fun State.toViewState(): ViewState {
        return ViewState(
            userMessage = userMessage,
            userMessageField = userMessageField
        )
    }

    sealed class Action {
        data object NotifyMessageSent : Action()
        data class Toast(@StringRes val id: Int) : Action()
    }

    sealed class Event {
        data object LoadUserMessage : Event()
        data class OnUserMessageLoaded(val message: String) : Event()
        data class WriteMessage(val message: String) : Event()
        data object SendMessage : Event()
        data class OnMessageSent(val message: String) : Event()
        data class OnMessageError(@StringRes val errorId: Int) : Event()
        data object ClearMessage : Event()
    }

    data class State(
        val userMessage: String = "",
        val userMessageField: String = ""
    )

    @Immutable
    data class ViewState(
        val userMessage: String,
        val userMessageField: String,
    )

    companion object {
        private const val TAG = "MESSAGE_VM"
    }
}
