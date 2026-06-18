package com.kumpello.whereiseveryone.common.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : Any, VS : Any, E : Any, Ef : Any>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<VS> = _state
        .map { it.toViewState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialState.toViewState()
        )

    private val _effect = MutableSharedFlow<Ef>()
    val action: SharedFlow<Ef> = _effect.asSharedFlow()

    protected val currentState: S
        get() = _state.value

    fun trigger(event: E) {
        val result = reduce(_state.value, event)
        _state.value = result.newState
        result.sideEffects.forEach { handleSideEffect(it) }
    }

    private fun handleSideEffect(sideEffect: SideEffect<E, Ef>) {
        when (sideEffect) {
            is SideEffect.Effect -> {
                viewModelScope.launch { _effect.emit(sideEffect.effect) }
            }
            is SideEffect.InternalEvent -> {
                trigger(sideEffect.event)
            }
            is SideEffect.AsyncWork -> {
                viewModelScope.launch {
                    val event = sideEffect.work()
                    trigger(event)
                }
            }
        }
    }

    protected abstract fun reduce(state: S, event: E): ReducerResult<S, E, Ef>
    protected abstract fun S.toViewState(): VS

    sealed class SideEffect<out E, out Ef> {
        data class Effect<Ef>(val effect: Ef) : SideEffect<Nothing, Ef>()
        data class InternalEvent<E>(val event: E) : SideEffect<E, Nothing>()
        data class AsyncWork<E>(val work: suspend () -> E) : SideEffect<E, Nothing>()
    }

    data class ReducerResult<S, E, Ef>(
        val newState: S,
        val sideEffects: List<SideEffect<E, Ef>> = emptyList()
    )

    // Helper functions for ReducerResult creation
    protected fun S.toResult(vararg sideEffects: SideEffect<E, Ef>): ReducerResult<S, E, Ef> {
        return ReducerResult(this, sideEffects.toList())
    }
}
