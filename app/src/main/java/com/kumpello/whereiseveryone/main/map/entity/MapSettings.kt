package com.kumpello.whereiseveryone.main.map.entity

data class MapSettings(
    val zoom: Double = 2.0,
    val zoomLocked: Boolean = false, //TODO: Change to max zoom?
    val bearing: Double = 0.0,
)
