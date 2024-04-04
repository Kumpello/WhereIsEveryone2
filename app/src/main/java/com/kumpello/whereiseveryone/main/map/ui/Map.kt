package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kumpello.whereiseveryone.main.friends.model.Location
import com.kumpello.whereiseveryone.main.map.domain.toPoint
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState

@OptIn(MapboxExperimental::class)
@Composable
fun Map(
    modifier: Modifier = Modifier,
    state: MapSettings,
    userLocation: Location,
    //friendsPositions: List<UserPosition>
) {
        MapboxMap(
        modifier.fillMaxSize(),
        mapViewportState = MapViewportState().apply {
            setCameraOptions {
                zoom(state.zoom)
                center(userLocation.toPoint())
                pitch(0.0)
                bearing(userLocation.bearing)
            }
        },
    )
}

/*@Preview
@Composable
fun MapPreview() {
    Map(
        state = MapSettings(),
        userLocation = Location()
    )
}*/
