package com.kumpello.whereiseveryone.ui.main

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.kumpello.whereiseveryone.data.model.map.MapUIState
import com.kumpello.whereiseveryone.domain.events.GetPositionsEvent
import com.kumpello.whereiseveryone.domain.events.UIEvent
import com.kumpello.whereiseveryone.domain.usecase.FriendsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {

    var _uiState = mutableStateOf(MapUIState())
    val uiState: State<MapUIState> = _uiState
    lateinit var event: MutableSharedFlow<GetPositionsEvent>
    private lateinit var friendsService: FriendsService

    fun onEvent(event: UIEvent) {
        when (event) {
            is UIEvent.SwitchControl -> {
                _uiState.value = _uiState.value.copy(
                    mapUiSettings = _uiState.value.mapUiSettings.copy(
                        zoomControlsEnabled = event.value,
                        scrollGesturesEnabled = event.value,
                        tiltGesturesEnabled = event.value,
                        zoomGesturesEnabled = event.value,
                        rotationGesturesEnabled = event.value
                    )
                )
            }

            is UIEvent.AddFriend -> {
                friendsService.addFriend(event.application, event.nick)
            }
        }
    }

    fun setFriendsService(friendsService: FriendsService) {
        this.friendsService = friendsService
    }

}