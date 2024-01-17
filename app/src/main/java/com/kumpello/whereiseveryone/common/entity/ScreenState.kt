package com.kumpello.whereiseveryone.common.entity

sealed class ScreenState {
    data object Loading : ScreenState()
    data object Success : ScreenState()
    data object Error : ScreenState()
}