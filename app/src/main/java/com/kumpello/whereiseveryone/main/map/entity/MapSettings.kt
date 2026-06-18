package com.kumpello.whereiseveryone.main.map.entity

import androidx.compose.runtime.Immutable

@Immutable
data class MapSettings(
    val zoom: Double = 16.5,
    val zoomLocked: Boolean = false, //TODO: Change to max zoom?
)
