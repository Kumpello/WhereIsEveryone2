package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.theme.USER_PUCK_COLOR
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.kumpello.whereiseveryone.main.map.extension.colorForUsername
import com.kumpello.whereiseveryone.main.map.extension.createAvatarBitmap
import com.kumpello.whereiseveryone.main.map.extension.createTintedBitmap
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxLocationComponentException
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.ImageValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconPitchAlignmentValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconRotationAlignmentValue
import com.mapbox.maps.extension.compose.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.compose.style.rememberStyleImage
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(MapboxExperimental::class)
@Composable
fun Map(
    modifier: Modifier = Modifier,
    state: MapSettings,
    actions: Flow<MapViewModel.Action>,
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

    LaunchedEffect(mapViewportState) {
        snapshotFlow { mapViewportState.cameraState?.bearing ?: 0.0 }
            .map { it.roundToInt() }
            .distinctUntilChanged()
            .collect { bearing ->
                event(MapViewModel.Event.OnCameraUpdate(bearing.toDouble()))
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
        val context = LocalContext.current
        val puckAvatarBitmap = remember { createAvatarBitmap(null, USER_PUCK_COLOR, sizePx = 120) }
        val puckBearingBitmap = remember {
            createTintedBitmap(context, R.drawable.ic_map_friend_ring_sdf, USER_PUCK_COLOR, sizePx = 270) //TODO: Implement size as accuracy? To consider
        }

        MapEffect(puckAvatarBitmap, puckBearingBitmap, friendsPositions.isNotEmpty()) { mapView ->
            val style = mapView.mapboxMap.style
            val ringLayerId = "friends-ring-layer"

            // The ring layer is added declaratively by FriendsSymbolLayer (in the style's topSlot).
            // There's no ordering guarantee it already exists when this effect runs, so wait briefly.
            var hasRingLayer = false
            if (friendsPositions.isNotEmpty()) {
                var attempts = 0
                while (!hasRingLayer && attempts < 30) { // ~480ms max
                    hasRingLayer = style?.styleLayerExists(ringLayerId) == true
                    if (!hasRingLayer) delay(16.milliseconds)
                    attempts++
                }
            }

            try {
                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = LocationPuck2D(
                        topImage = ImageHolder.from(puckAvatarBitmap),
                        bearingImage = ImageHolder.from(puckBearingBitmap),
                        shadowImage = null,
                    )
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.HEADING
                    if (hasRingLayer) {
                        layerBelow = ringLayerId
                    } else {
                        layerBelow = null
                        slot = "top"
                    }
                }
            } catch (e: MapboxLocationComponentException) {
                // Belt-and-suspenders: if the layer still wasn't there, don't crash — fall back to top slot.
                Timber.tag("MAP_UI").w(e, "Couldn't bind puck below $ringLayerId, falling back to top slot")
                mapView.location.updateSettings {
                    layerBelow = null
                    slot = "top"
                }
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
    val currentOnFriendClick by rememberUpdatedState(onFriendClick)
    val currentOnFriendLongClick by rememberUpdatedState(onFriendLongClick)

    val displayedFriends = remember { mutableStateMapOf<String, AnimatedFriendData>() }

    MapEffect(friends) { mapView ->
        val style = mapView.mapboxMap.style ?: return@MapEffect
        friends.forEach { friend ->
            val imageId = "avatar-${friend.username}"
            if (style.getStyleImage(imageId) == null) {
                val bitmap = createAvatarBitmap(
                    name = friend.username,
                    backgroundColor = colorForUsername(friend.username),
                    sizePx = 150
                )
                style.addImage(imageId, bitmap)
            }
        }
    }

    // NOTE: this loop only reads `friends` (the data param) and writes to `displayedFriends`
    // (never reads it). It must NOT live in the same recomposition scope as anything that
    // reads `displayedFriends`, or every animation frame will re-trigger it and restart
    // every friend's tween. See UpdateFriendsGeoJsonSource below.
    friends.forEach { friend ->
        val location = friend.location ?: return@forEach
        val target = AnimatedFriendData(
            lat = location.lat,
            lon = location.lon,
            bearing = location.bearing?.toDouble() ?: 0.0,
            opacity = location.lastUpdateAge.opacity,
            haloWidth = location.accuracy.haloSize,
            speed = location.speed?.toDouble() ?: 0.0
        )
        key(friend.username) {
            AnimatedFriendEffect(
                id = friend.username,
                target = target,
                onUpdate = { updated -> displayedFriends[friend.username] = updated },
                onRemoved = { displayedFriends.remove(friend.username) }
            )
        }
    }

    val sourceState = rememberGeoJsonSourceState()

    // Isolated in its own composable so *only this scope* recomposes when displayedFriends
    // changes on every animation frame — it can no longer cascade back into the forEach above.
    UpdateFriendsGeoJsonSource(displayedFriends = displayedFriends, sourceState = sourceState)

    val friendRing = rememberStyleImage(
        imageId = "friend-ring",
        resourceId = R.drawable.ic_map_friend_ring_sdf,
        sdf = true
    )
    val friendRingNotchless = rememberStyleImage(
        imageId = "friend-ring-notchless",
        resourceId = R.drawable.ic_map_friend_ring_notchless_sdf,
        sdf = true
    )

    SymbolLayer(
        sourceState = sourceState,
        layerId = "friends-ring-layer",
    ) {
        iconImage = ImageValue(
            Expression.switchCase(
                Expression.lt(Expression.get("speed"), Expression.literal(1.0)),
                Expression.literal(friendRingNotchless.imageId),
                Expression.literal(friendRing.imageId)
            )
        )
        iconSize = DoubleValue(Expression.get("haloWidth"))
        iconRotate = DoubleValue(Expression.get("bearing"))
        iconRotationAlignment = IconRotationAlignmentValue.MAP
        iconPitchAlignment = IconPitchAlignmentValue.MAP
        iconOpacity = DoubleValue(Expression.get("opacity"))
        iconColor = ColorValue(Expression.toColor(Expression.get("color")))
        iconAllowOverlap = BooleanValue(true)
        iconIgnorePlacement = BooleanValue(true)
    }

    SymbolLayer(
        sourceState = sourceState,
        layerId = "friends-avatar-layer"
    ) {
        iconImage = ImageValue(Expression.get("avatarId"))
        iconSize = DoubleValue(1.0)
        iconRotationAlignment = IconRotationAlignmentValue.VIEWPORT
        iconPitchAlignment = IconPitchAlignmentValue.MAP
        iconOpacity = DoubleValue(Expression.get("opacity"))
        iconAllowOverlap = BooleanValue(true)
        iconIgnorePlacement = BooleanValue(true)

        interactionsState.onClicked { feature, _ ->
            val id = feature.properties.getString("id")
            friendsByIdState.value[id]?.let(currentOnFriendClick)
            true
        }
        interactionsState.onLongClicked { feature, _ ->
            val id = feature.properties.getString("id")
            friendsByIdState.value[id]?.let(currentOnFriendLongClick)
            true
        }
    }
}

/**
 * Deliberately its own composable. `displayedFriends` is written to on every animation frame
 * (via AnimatedFriendEffect's onUpdate), so reading it here — and ONLY here — means those
 * writes invalidate just this small scope instead of the whole FriendsSymbolLayer body
 * (which would otherwise re-run the friend/target-building loop every frame).
 */
@OptIn(MapboxExperimental::class)
@Composable
private fun UpdateFriendsGeoJsonSource(
    displayedFriends: Map<String, AnimatedFriendData>,
    sourceState: GeoJsonSourceState,
) {
    val features = displayedFriends.map { (id, data) ->
        Feature.fromGeometry(Point.fromLngLat(data.lon, data.lat)).apply {
            addStringProperty("id", id)
            addStringProperty("avatarId", "avatar-$id")
            addNumberProperty("bearing", data.bearing)
            addNumberProperty("opacity", data.opacity)
            addNumberProperty("haloWidth", data.haloWidth)
            addNumberProperty("speed", data.speed)
            addStringProperty("color", String.format("#%06X", 0xFFFFFF and colorForUsername(id)))
        }
    }
    sourceState.data = GeoJSONData(features)
}
