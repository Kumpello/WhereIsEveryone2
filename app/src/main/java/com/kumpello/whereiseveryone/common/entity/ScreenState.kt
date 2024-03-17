package com.kumpello.whereiseveryone.common.entity

sealed class ScreenState {
    data object Loading : ScreenState()
    data object Map : ScreenState()
    data object Settings : ScreenState()
    data object Friends : ScreenState()
    data object Error : ScreenState()
}