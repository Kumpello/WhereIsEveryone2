package com.kumpello.whereiseveryone.domain.events

sealed class UIEvent {
    data class SwitchControl(val value : Boolean) : UIEvent()
}
