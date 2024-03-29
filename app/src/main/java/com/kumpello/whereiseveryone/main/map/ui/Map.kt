package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState

@OptIn(MapboxExperimental::class)
@Composable
fun Map(
    modifier: Modifier = Modifier,
    state: MapSettings
) {
    MapboxMap(
        modifier.fillMaxSize(),
        mapViewportState = MapViewportState().apply {
            setCameraOptions {
                zoom(state.zoom)
                center(Point.fromLngLat(-98.0, 39.5))
                pitch(0.0)
                bearing(0.0)
            }
        },
    )
}

@Preview
@Composable
fun MapPreview() {
    Map(
        state = MapSettings()
    )
}
