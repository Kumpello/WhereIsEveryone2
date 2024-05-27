package com.kumpello.whereiseveryone.common.entity

import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel

sealed class ScreenState {
    data object Loading : ScreenState()
    data object Map : ScreenState()

    data object Message : ScreenState()
    data class Settings(val settingsViewModel: SettingsViewModel) : ScreenState()
    data object Friends : ScreenState()
    data object Error : ScreenState()
}