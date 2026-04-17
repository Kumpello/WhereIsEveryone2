package com.kumpello.whereiseveryone.authentication.splash.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SplashViewModel(
    private val getCurrentAuthKeyUseCase: GetCurrentAuthKeyUseCase
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

    private fun isUserLogged() : Boolean {
        //TODO: This needs to be done some other way
        return getCurrentAuthKeyUseCase.execute().isNullOrEmpty().not()
    }

    private fun navigateToNextDestination() {
        viewModelScope.launch {
            if (isUserLogged()) {
                _action.emit(Action.NavigateMain)
            } else {
                _action.emit(Action.NavigateSignUp)
            }
        }
    }

    fun trigger(command: Command) {
        when (command) {
            Command.NavigateToNextDestination -> navigateToNextDestination()
        }
    }

    private fun State.toViewState(): ViewState {
        return ViewState(
            scale = scale,
        )
    }

    sealed class Action {
        data object NavigateSignUp: Action()
        data object NavigateMain: Action()

    }

    sealed class Command {
        data object NavigateToNextDestination : Command()
    }

    data class State(
        val scale : Animatable<Float, AnimationVector1D> = Animatable(0f)
    )

    data class ViewState(
        val scale : Animatable<Float, AnimationVector1D>
    )

}
