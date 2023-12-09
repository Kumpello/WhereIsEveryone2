package com.kumpello.whereiseveryone.authentication.splash.presentation

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {
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

    private fun animateLogo() {
        viewModelScope.launch {
            _state.value.scale.animateTo(
                targetValue = 0.9f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = {
                        OvershootInterpolator(2f).getInterpolation(it)
                    })
            )
        }
    }

    fun trigger(command: Command) {
        when (command) {
            Command.AnimateLogo -> animateLogo()
        }
    }

    private fun State.toViewState(): ViewState {
        return ViewState(
            scale = scale,
        )
    }

    sealed class Action {

    }

    sealed class Command {
        data object AnimateLogo : Command()
    }

    data class State(
        val scale : Animatable<Float, AnimationVector1D> = Animatable(0f)
    )

    data class ViewState(
        val scale : Animatable<Float, AnimationVector1D>
    )

}
