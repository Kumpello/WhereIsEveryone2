package com.kumpello.whereiseveryone.common.entities

sealed class ScreenState {
    data object Loading : ScreenState()
    data object Success : ScreenState()
    data object Error : ScreenState()
}