package com.kumpello.whereiseveryone.map.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings

data class MapUIState(
    val position: LatLng = LatLng(0.0, 0.0),
    val zoom: Float = 10f,
    val mapUiSettings: MapUiSettings = MapUiSettings(),
    val properties : MapProperties = MapProperties(mapType = MapType.SATELLITE),
    val controlState : Boolean = true
)
