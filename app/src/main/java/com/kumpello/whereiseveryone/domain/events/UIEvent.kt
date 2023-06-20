package com.kumpello.whereiseveryone.domain.events

import android.content.Context

sealed class UIEvent {
    data class SwitchControl(val value : Boolean) : UIEvent()
    data class AddFriend(val context: Context, val nick : String) : UIEvent()
}
