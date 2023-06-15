package com.kumpello.whereiseveryone.ui.main

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.kumpello.whereiseveryone.data.model.map.MapUIState
import com.kumpello.whereiseveryone.domain.events.GetPositionsEvent
import com.kumpello.whereiseveryone.domain.usecase.PositionsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {

    var _uiState = mutableStateOf(MapUIState(Any()))
    val uiState: State<MapUIState> = _uiState
    lateinit var event : MutableSharedFlow<GetPositionsEvent>

    fun onEvent(event: GetPositionsEvent) {
        when(event) {
            is GetPositionsEvent.GetError -> TODO()
            is GetPositionsEvent.GetSuccess -> TODO()
        }
    }

    fun setEvent(event : MutableSharedFlow<GetPositionsEvent>) {
        this.event = event
    }

}