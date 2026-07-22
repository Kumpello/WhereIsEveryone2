package com.kumpello.whereiseveryone.main.map.ui

import android.graphics.Color.GREEN
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.rememberStyleImage
import com.mapbox.maps.extension.compose.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.compose.style.layers.generated.SymbolLayerState
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleListValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.FormattedValue
import com.mapbox.maps.extension.compose.style.layers.ImageValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconRotationAlignmentValue
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(MapboxExperimental::class)
@Composable
fun Map(
    modifier: Modifier = Modifier,
    state: MapSettings,
    actions: Flow<MapViewModel.Action>,
    userLocation: Location?,
    friendsPositions: List<Friend>,
    event: (MapViewModel.Event) -> Unit,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.map_placeholder),
                color = androidx.compose.ui.graphics.Color.White
            )
        }
        return
    }

    val mapViewportState = rememberMapViewportState {
        transitionToFollowPuckState(
            followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder()
                .zoom(state.zoom).build()
        )
    }
    val mapState = rememberMapState {
        gesturesSettings = GesturesSettings {
            rotateEnabled = true
            pinchToZoomEnabled = true
            pitchEnabled = true
        }
    }

    LaunchedEffect(actions) {
        actions.collect { action ->
            when (action) {
                is MapViewModel.Action.CenterMap -> {
                    mapViewportState.transitionToFollowPuckState(
                        followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder()
                            .zoom(action.zoom).build(),
                        defaultTransitionOptions = DefaultViewportTransitionOptions.Builder()
                            .maxDurationMs(500L).build()
                    )
                }

                is MapViewModel.Action.Zoom -> {
                    Timber.tag("MAP_UI").d("Zooming to: %s", action.zoom)
                    mapViewportState.transitionToFollowPuckState(
                        followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder()
                            .zoom(action.zoom).build(),
                        defaultTransitionOptions = DefaultViewportTransitionOptions.Builder()
                            .maxDurationMs(50L).build()
                    )
                }

                else -> Unit
            }
        }
    }

    MapboxMap(
        modifier.fillMaxSize(),
        mapViewportState = mapViewportState,
        mapState = mapState,
        scaleBar = {
            ScaleBar(
                modifier = Modifier
                    .safeDrawingPadding(),
                ratio = 0.3F,
                height = 4.dp,
                textSize = 16.sp
            )
        },
        compass = {
            Compass(
                modifier = Modifier
                    .safeDrawingPadding()
                    .size(58.dp),
                alignment = Alignment.BottomStart
            )
        },
        logo = {
            Logo(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(
                        start = 64.dp,
                        bottom = 4.dp
                    )
            )
        },
        attribution = {
            Attribution(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(
                        start = 64.dp,
                        bottom = 4.dp
                    )
            )
        },
        style = {
            MapboxStandardStyle(
                topSlot = {
                    FriendsSymbolLayer(
                        friends = friendsPositions,
                        onFriendClick = { friend -> event(MapViewModel.Event.OnFriendClick(friend)) },
                        onFriendLongClick = { friend ->
                            event(
                                MapViewModel.Event.OnFriendLongClick(
                                    friend
                                )
                            )
                        }
                    )
                }
            )
        }
    ) {
        MapEffect(Unit) { mapView ->
            mapView.location.updateSettings {
                locationPuck = createDefault2DPuck(withBearing = true)
                puckBearingEnabled = true
                puckBearing = PuckBearing.HEADING
                enabled = true
            }
        }
    }
}

@OptIn(MapboxExperimental::class, MapboxDelicateApi::class)
@Composable
fun FriendsSymbolLayer(
    friends: List<Friend>,
    onFriendClick: (Friend) -> Unit,
    onFriendLongClick: (Friend) -> Unit
) {
    val friendsById = remember(friends) {
        friends.associateBy { it.username }
    }
    val friendsByIdState = rememberUpdatedState(friendsById)

    val featureCollection = remember(friends) {
        FeatureCollection.fromFeatures(
            friends.mapNotNull { friend ->
                friend.location?.let { loc ->
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            loc.lon,
                            loc.lat
                        )
                    ).apply {
                        addStringProperty(
                            "id",
                            friend.username
                        )
                        addNumberProperty(
                            "bearing",
                            loc.bearing ?: 0.0
                        )
                        addNumberProperty(
                            "opacity",
                            loc.lastUpdateAge.opacity
                        )
                        addNumberProperty(
                            "haloWidth",
                            loc.accuracy.haloSize
                        )
                    }
                }
            }
        )
    }

    val sourceState = rememberGeoJsonSourceState {
        data = GeoJSONData(featureCollection.features()!!)
    }

    LaunchedEffect(featureCollection) {
        sourceState.data = GeoJSONData(featureCollection.features()!!)
    }

    val friendMarker = rememberStyleImage(
        imageId = "friend-arrow",
        resourceId = R.drawable.ic_map_friend_sdf,
        sdf = true
    )

    SymbolLayer(
        sourceState = sourceState,
        layerId = "friends-layer",
        symbolLayerState = remember {
            SymbolLayerState().apply {
                iconImage = ImageValue(friendMarker)
                iconSize = DoubleValue(1.0)
                iconRotate = DoubleValue(Expression.get("bearing"))
                iconRotationAlignment = IconRotationAlignmentValue.MAP
                iconOpacity = DoubleValue(Expression.get("opacity"))
                iconColor = ColorValue(Expression.color(android.graphics.Color.BLACK))

                iconHaloColor = ColorValue(
                    Expression.match(
                        Expression.get("haloWidth"),
                        Expression.literal(-1.0), Expression.color(android.graphics.Color.RED),
                        Expression.color(
                            ColorUtils.setAlphaComponent(
                                GREEN, iconOpacity.doubleOrNull?.coerceIn(
                                    0.0, 1.0 * 255
                                )?.roundToInt() ?: 255
                            )
                        ),
                    )
                )
                iconHaloWidth = DoubleValue(Expression.get("haloWidth"))
                iconHaloBlur = DoubleValue(2.0)

                iconAllowOverlap = BooleanValue(true)
                iconIgnorePlacement = BooleanValue(true)

                textField = FormattedValue(Expression.get("id"))
                textOffset = DoubleListValue(listOf(0.0, -2.5))

                interactionsState.onClicked { feature, _ ->
                    val id = feature.properties.getString("id")
                    val friend = friendsByIdState.value[id]
                    friend?.let(onFriendClick)
                    true
                }
                interactionsState.onLongClicked { feature, _ ->
                    val id = feature.properties.getString("id")
                    val friend = friendsByIdState.value[id]
                    friend?.let(onFriendLongClick)
                    true
                }
            }
        }
    )
}
