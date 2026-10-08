package com.kumpello.whereiseveryone.core.presentation

import androidx.compose.runtime.Stable

@Stable
sealed class ScreenState {
    data object Loading : ScreenState()
    data object Map : ScreenState()

    data object Message : ScreenState()
    data object Error : ScreenState()
}