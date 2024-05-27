package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.main.common.domain.model.Location
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.generated.LocationComponentSettings
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
    val density = LocalDensity.current.density

    //DefaultSettingsProvider may be useful
    //FollowPuckViewportState also
    //TODO: Use passed user location data!
    //TODO: removal/change of logo
    val locationComponentSettings: LocationComponentSettings by remember {
        mutableStateOf(
            LocationComponentSettings(
                locationPuck = createDefault2DPuck(withBearing = true), //TODO: Change to 3D?
                initializer = {
                    enabled = true
                    puckBearingEnabled = true
                    pulsingMaxRadius *= density
                    puckBearing =
                        PuckBearing.HEADING //TODO: Change depending on move or not? Or switch by button?
                }
            )
        )
    }

    val mapBoxUiSettings: GesturesSettings by remember {
        mutableStateOf(
            GesturesSettings {
                rotateEnabled = true
                pinchToZoomEnabled = true
                pitchEnabled = true
            }
        )
    }

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
            }
        }
    }

    MapboxMap(
        modifier.fillMaxSize(),
        mapViewportState = mapViewportState,
        locationComponentSettings = locationComponentSettings,
        gesturesSettings = mapBoxUiSettings,
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
