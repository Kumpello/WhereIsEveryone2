package com.kumpello.whereiseveryone.ui.main

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.kumpello.whereiseveryone.data.model.map.MapUIState
import com.kumpello.whereiseveryone.domain.events.GetPositionsEvent
import com.kumpello.whereiseveryone.domain.events.UIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {

    var _uiState = mutableStateOf(MapUIState())
    val uiState: State<MapUIState> = _uiState
    private lateinit var event: MutableSharedFlow<GetPositionsEvent>
    lateinit var lastPosition: LatLng

    /*    fun onEvent(event: GetPositionsEvent) {
            when(event) {
                is GetPositionsEvent.GetError -> TODO()
                is GetPositionsEvent.GetSuccess -> TODO()
            }
        }*/

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
        }
    }

    fun setEvent(event: MutableSharedFlow<GetPositionsEvent>) {
        this.event = event
    }

}