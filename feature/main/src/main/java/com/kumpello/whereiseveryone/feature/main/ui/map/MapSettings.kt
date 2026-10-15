package com.kumpello.whereiseveryone.feature.main.ui.map

import androidx.compose.runtime.Immutable
import com.kumpello.whereiseveryone.feature.main.ui.model.MapFpsLimit

@Immutable
data class MapSettings(
    val zoom: Double = 16.5,
    val zoomLocked: Boolean = false, //TODO: Change to max zoom?
    val bearing: Double = 0.0,
    val fpsLimit: MapFpsLimit = MapFpsLimit.FPS_120,
)
