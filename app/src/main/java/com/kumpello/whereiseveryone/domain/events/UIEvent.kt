package com.kumpello.whereiseveryone.domain.events

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication

sealed class UIEvent {
    data class SwitchControl(val value : Boolean) : UIEvent()
    data class AddFriend(val application: WhereIsEveryoneApplication, val nick : String) : UIEvent()
}
