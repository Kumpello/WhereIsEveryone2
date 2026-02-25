package com.kumpello.whereiseveryone.common.entity

sealed class ScreenState {
    data object Loading : ScreenState()
    data object Map : ScreenState()

    data object Message : ScreenState()
    data object Error : ScreenState()
}