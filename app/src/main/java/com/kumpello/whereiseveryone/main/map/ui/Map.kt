package com.kumpello.whereiseveryone.main.map.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.extension.lerp
import com.kumpello.whereiseveryone.common.extension.lerpBearing
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.MapboxExperimental
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
import com.mapbox.maps.extension.compose.style.layers.generated.SymbolLayerState
import com.mapbox.maps.extension.compose.style.rememberStyleImage
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
            createTintedBitmap(context, R.drawable.ic_map_friend_ring_sdf, USER_PUCK_COLOR, sizePx = 270)
        }

        MapEffect(puckAvatarBitmap, puckBearingBitmap) { mapView ->
            mapView.location.updateSettings {
                enabled = true
                locationPuck = LocationPuck2D(
                    topImage = ImageHolder.from(puckAvatarBitmap),
                    bearingImage = ImageHolder.from(puckBearingBitmap),
                    shadowImage = null,
                )
                puckBearingEnabled = true
                puckBearing = PuckBearing.HEADING
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

    var displayedFriends by remember { mutableStateOf<Map<String, AnimatedFriendData>>(emptyMap()) }

    LaunchedEffect(friends) {
        val duration = 1000L // 1 second transition
        val startTime = withFrameNanos { it } / 1_000_000
        val startStates = displayedFriends

        val targetStates = friends.mapNotNull { friend ->
            friend.location?.let { loc ->
                friend.username to AnimatedFriendData(
                    lat = loc.lat,
                    lon = loc.lon,
                    bearing = loc.bearing?.toDouble() ?: 0.0,
                    opacity = loc.lastUpdateAge.opacity,
                    haloWidth = loc.accuracy.haloSize
                )
            }
        }.toMap()

        if (startStates.isEmpty()) {
            displayedFriends = targetStates
            return@LaunchedEffect
        }

        var playTime = 0L
        while (playTime < duration) {
            playTime = (withFrameNanos { it } / 1_000_000) - startTime
            val fraction = (playTime.toFloat() / duration).coerceIn(0f, 1f)

            displayedFriends = targetStates.mapValues { (id, target) ->
                val start = startStates[id] ?: target
                AnimatedFriendData(
                    lat = lerp(start.lat, target.lat, fraction.toDouble()),
                    lon = lerp(start.lon, target.lon, fraction.toDouble()),
                    bearing = lerpBearing(start.bearing, target.bearing, fraction.toDouble()),
                    opacity = lerp(start.opacity, target.opacity, fraction.toDouble()),
                    haloWidth = lerp(start.haloWidth, target.haloWidth, fraction.toDouble())
                )
            }
        }
        displayedFriends = targetStates
    }

    val registeredAvatars = remember { mutableSetOf<String>() }
    MapEffect(friends) { mapView ->
        val style = mapView.mapboxMap.style ?: return@MapEffect
        friends.forEach { friend ->
            val imageId = "avatar-${friend.username}"
            if (registeredAvatars.add(imageId)) {
                val bitmap = createAvatarBitmap(
                    name = friend.username,
                    backgroundColor = colorForUsername(friend.username),
                    sizePx = 150
                )
                style.addImage(imageId, bitmap) // non-SDF: full-color image, not tinted
            }
        }
    }

    val sourceState = rememberGeoJsonSourceState()

    LaunchedEffect(displayedFriends) {
        val features = displayedFriends.map { (id, data) ->
            Feature.fromGeometry(
                Point.fromLngLat(
                    data.lon,
                    data.lat
                )
            ).apply {
                addStringProperty(
                    "id",
                    id
                )
                addStringProperty("avatarId", "avatar-$id")
                addNumberProperty(
                    "bearing",
                    data.bearing
                )
                addNumberProperty(
                    "opacity",
                    data.opacity
                )
                addNumberProperty(
                    "haloWidth",
                    data.haloWidth
                )
                addStringProperty(
                    "color",
                    String.format("#%06X", 0xFFFFFF and colorForUsername(id))
                )
            }
        }
        sourceState.data = GeoJSONData(features)
    }

    SymbolLayer(
        sourceState = sourceState,
        layerId = "friends-avatar-layer"
    ) {
        iconImage = ImageValue(Expression.get("avatarId"))
        iconSize = DoubleValue(1.0)
        iconRotationAlignment = IconRotationAlignmentValue.VIEWPORT
        iconPitchAlignment = IconPitchAlignmentValue.MAP
        iconOpacity =
            DoubleValue(Expression.get("opacity")) // fades in sync with your existing layer
        iconAllowOverlap = BooleanValue(true)
        iconIgnorePlacement = BooleanValue(true)
    }

    val friendRing = rememberStyleImage(
        imageId = "friend-arrow",
        resourceId = R.drawable.ic_map_friend_ring_sdf,
        sdf = true
    )

    SymbolLayer(
        sourceState = sourceState,
        layerId = "friends-ring-layer",
        symbolLayerState = remember {
            SymbolLayerState().apply {
                iconImage = ImageValue(friendRing)
                iconSize = DoubleValue(Expression.get("haloWidth"))
                iconRotate = DoubleValue(Expression.get("bearing"))
                iconRotationAlignment = IconRotationAlignmentValue.MAP
                iconPitchAlignment = IconPitchAlignmentValue.MAP
                iconOpacity = DoubleValue(Expression.get("opacity"))
                iconColor = ColorValue(Expression.toColor(Expression.get("color")))

                iconAllowOverlap = BooleanValue(true)
                iconIgnorePlacement = BooleanValue(true)

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

private fun createAvatarBitmap(
    name: String?,
    backgroundColor: Int,
    sizePx: Int = 130
): Bitmap {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint)
    if (!name.isNullOrBlank()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.42f
            val textWidth = measureText(name.uppercase())
            val maxWidth = sizePx * 0.8f
            if (textWidth > maxWidth) {
                textSize *= maxWidth / textWidth
            }
        }
        val textY = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(name.uppercase(), sizePx / 2f, textY, textPaint)
    }
    return bitmap
}

private fun createTintedBitmap(
    context: android.content.Context,
    @androidx.annotation.DrawableRes resourceId: Int,
    tintColor: Int,
    sizePx: Int = 130
): Bitmap {
    val drawable = ContextCompat.getDrawable(context, resourceId)!!.mutate()
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, sizePx, sizePx)
    drawable.setTint(tintColor)
    drawable.draw(canvas)
    return bitmap
}

private fun colorForUsername(username: String): Int {
    val hue = (username.hashCode() and 0x7FFFFFFF % 360).toFloat()
    return android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.5f, 0.7f))
}

private val USER_PUCK_COLOR = "#3478F6".toColorInt()
