package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.main.common.domain.model.Location
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.flow.SharedFlow

@OptIn(MapboxExperimental::class)
@Composable
fun Map(
    modifier: Modifier = Modifier,
    state: MapSettings,
    actions: SharedFlow<MapViewModel.Action>,
    userLocation: Location,
    //friendsPositions: List<UserPosition>
) {
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            //center(userLocation.toPoint())
            //pitch(0.0)
            zoom(state.zoom)
        }
        transitionToFollowPuckState()
    }

    LaunchedEffect(actions) {
        actions.collect { action ->
            when (action) {
                MapViewModel.Action.CenterMap -> {
                    mapViewportState.transitionToFollowPuckState()
                }
                else -> Unit
            }
        }
    }

    MapboxMap(
        modifier.fillMaxSize(),
        mapViewportState = mapViewportState,
        scaleBar = {
            ScaleBar(
                modifier = Modifier
                    .safeDrawingPadding()
            )
        },
        compass = {
            Compass(
                modifier = Modifier
                    .safeDrawingPadding()
                    .size(54.dp),
                alignment = Alignment.BottomStart
            )
        },
        logo = {
            Logo(
                modifier = Modifier.padding(
                    start = 6.dp,
                    bottom = 2.dp
                )
            )
        }
    ) {
        //TODO: Use passed user location data!
        //TODO: removal/change of logo
        MapEffect(Unit) { mapView ->
            mapView.location.updateSettings {
                locationPuck = createDefault2DPuck(withBearing = true)
                puckBearingEnabled = true
                puckBearing = PuckBearing.HEADING
                enabled = true
            }
            mapView.gestures.updateSettings {
                rotateEnabled = true
                pinchToZoomEnabled = true
                pitchEnabled = true
            }
        }
    }
}
