package com.kumpello.whereiseveryone.main.map.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.ui.rememberBitmapFromDrawable
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
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.extension.compose.style.rememberStyleState
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

@OptIn(MapboxExperimental::class)
@Composable
fun Map(
    modifier: Modifier = Modifier,
    state: MapSettings,
    actions: SharedFlow<MapViewModel.Action>,
    userLocation: Location,
    friendsPositions: List<Friend>,
    event: (MapViewModel.Event) -> Unit,
) {
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
    val friendsById = remember(friendsPositions) {
        friendsPositions.associateBy { it.username }
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
                    Timber.d(action.zoom.toString())
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
        },
        style = {
            GenericStyle(
                styleState = rememberStyleState {

                    styleInteractionsState
                        .onLayerClicked("friends-layer") { feature, _ ->
                            feature.properties.getString("id")
                                .let(friendsById::get)
                                ?.let{ friend -> MapViewModel.Event.OnFriendClick(friend) }

                            true
                        }
                        .onLayerLongClicked("friends-layer") { feature, _ ->
                            feature.properties.getString("id")
                                .let(friendsById::get)
                                ?.let{ friend -> MapViewModel.Event.OnFriendLongClick(friend) }

                            true
                        }
                },
                style = "mapbox://styles/mapbox/standard"
            )
        }
    ) {
        //TODO: Use passed user location data!
        //TODO: removal/change of logo
        //TODO: Move UI here?
        MapEffect(Unit) { mapView ->
            mapView.location.updateSettings {
                locationPuck = createDefault2DPuck(withBearing = true)
                puckBearingEnabled = true
                puckBearing = PuckBearing.HEADING
                enabled = true
            }
        }

        //TODO Check if finally fixed
        val friendMarker = rememberBitmapFromDrawable(R.drawable.ic_map_friend_sdf)
        FriendsSymbolLayer(friendMarker, friendsPositions)
    }
}

@Composable
fun FriendsSymbolLayer(
    friendMarker: Bitmap,
    friends: List<Friend>
) {
    val featureCollection = remember(friends) {
        FeatureCollection.fromFeatures(
            friends.map { friend ->

                Feature.fromGeometry(
                    Point.fromLngLat(
                        friend.location.lon,
                        friend.location.lat
                    )
                ).apply {

                    addStringProperty(
                        "id",
                        friend.username
                    )

                    addNumberProperty(
                        "bearing",
                        friend.location.bearing ?: 0.0
                    )

                    addNumberProperty(
                        "opacity",
                        friend.location.lastUpdateAge.opacity
                    )

                    addNumberProperty(
                        "haloWidth",
                        friend.location.accuracy.haloSize
                    )
                }
            }
        )
    }

    MapEffect(featureCollection) { mapView ->

        mapView.mapboxMap.getStyle { style ->

            if (!style.hasStyleImage("friend-arrow")) {
                style.addImage(
                    "friend-arrow",
                    friendMarker,
                    true
                )
            }

            val source =
                style.getSourceAs<GeoJsonSource>(
                    "friends-source"
                )

            if (source == null) {

                style.addSource(
                    geoJsonSource("friends-source") {
                        featureCollection(featureCollection)
                    }
                )

            } else {

                source.featureCollection(
                    featureCollection
                )
            }

            if (style.getLayer("friends-layer") == null) {

                style.addLayer(
                    symbolLayer(
                        "friends-layer",
                        "friends-source"
                    ) {

                        iconImage("friend-arrow")

                        iconSize(1.0)

                        iconRotate(
                            Expression.get("bearing")
                        )

                        iconRotationAlignment(
                            IconRotationAlignment.MAP
                        )

                        iconOpacity(
                            Expression.get("opacity")
                        )

                        iconColor(
                            Color.BLACK
                        )

                        iconHaloColor(
                            Color.RED //TODO Change if precision is UNKNOWN, settle for some color scheme
                        )

                        iconHaloWidth(
                            50.0//Expression.get("haloWidth")
                        )

                        iconAllowOverlap(true)

                        iconIgnorePlacement(true)

                        textField(
                            Expression.get("id")
                        )

                        textOffset(
                            listOf(0.0, -2.5)
                        )
                    }
                )
            }
        }
    }
}
