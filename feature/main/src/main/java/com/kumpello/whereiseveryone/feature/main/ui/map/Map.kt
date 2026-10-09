package com.kumpello.whereiseveryone.feature.main.ui.map

import com.kumpello.whereiseveryone.feature.main.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.core.ui.theme.USER_PUCK_COLOR
import com.kumpello.whereiseveryone.feature.main.ui.model.Friend
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxLocationComponentException
import com.mapbox.maps.coroutine.styleLoadedEvents
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
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.sources.addGeoJSONSourceFeatures
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.removeGeoJSONSourceFeatures
import com.mapbox.maps.extension.style.sources.updateGeoJSONSourceFeatures
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

private const val FRIENDS_RING_LAYER_ID = "friends-ring-layer"
private const val AVATAR_UPLOAD_BATCH_SIZE = 32

@OptIn(MapboxExperimental::class)
@Composable
fun Map(
    modifier: Modifier = Modifier,
    state: MapSettings,
    actions: Flow<MapViewModel.Action>,
    friendsPositions: List<Friend>,
    event: (MapViewModel.Event) -> Unit,
    trackCameraBearing: Boolean = true,
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
        gesturesState.gesturesSettings = GesturesSettings {
            rotateEnabled = true
            pinchToZoomEnabled = true
            pitchEnabled = true
        }
    }

    val currentEvent by rememberUpdatedState(event)
    LaunchedEffect(mapViewportState, trackCameraBearing) {
        if (!trackCameraBearing) return@LaunchedEffect
        snapshotFlow { mapViewportState.cameraState?.bearing ?: 0.0 }
            .map { it.roundToInt() }
            .distinctUntilChanged()
            .collect { bearing ->
                currentEvent(MapViewModel.Event.OnCameraUpdate(bearing.toDouble()))
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
            MapScaleBar()
        },
        compass = {
            MapCompass()
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
        val puckImages by produceState<Pair<android.graphics.Bitmap, android.graphics.Bitmap>?>(null, context) {
            value = withContext(Dispatchers.Default) {
                createAvatarBitmap(null, USER_PUCK_COLOR, sizePx = 120) to
                    createTintedBitmap(context, R.drawable.ic_map_friend_ring_sdf, USER_PUCK_COLOR, sizePx = 270)
            }
        }

        MapEffect(puckImages) { mapView ->
            val (puckAvatarBitmap, puckBearingBitmap) = puckImages ?: return@MapEffect
            val map = mapView.mapboxMap
            map.styleLoadedEvents.map { Unit }
                .onStart { if (map.style != null) emit(Unit) }
                .collect {
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
                            slot = "top"
                            layerBelow = FRIENDS_RING_LAYER_ID.takeIf(map::styleLayerExists)
                        }
                    } catch (e: MapboxLocationComponentException) {
                        Timber.tag("MAP_UI").w(e, "Couldn't bind puck below friend markers")
                        mapView.location.updateSettings {
                            layerBelow = null
                            slot = "top"
                        }
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

    // Animation writes are observed by a coroutine, so frames do not recompose these layers.
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

    // The source data is owned by UpdateFriendsGeoJsonSource's partial-update collector.
    // Avoid saving transient animated GeoJSON in the Activity's saved state.
    val sourceState = remember { GeoJsonSourceState() }

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

    val avatarUsernames = remember(friends) {
        friends.filter { it.location != null }.mapTo(mutableSetOf()) { it.username }
    }
    val currentAvatarUsernames by rememberUpdatedState(avatarUsernames)
    MapEffect(friendRing, friendRingNotchless) { mapView ->
        val map = mapView.mapboxMap
        map.styleLoadedEvents.map { Unit }
            .onStart { if (map.style != null) emit(Unit) }
            .collectLatest {
                // Expression image IDs do not register StyleImages automatically.
                listOf(friendRing, friendRingNotchless).forEach { image ->
                    map.addStyleImage(
                        imageId = image.imageId,
                        scale = image.scale ?: map.pixelRatio,
                        image = image.image,
                        sdf = image.sdf,
                        stretchX = image.stretchX,
                        stretchY = image.stretchY,
                        content = image.content,
                    ).onError { Timber.tag("MAP_UI").w("Unable to add marker ring: %s", it) }
                }
                val uploaded = mutableSetOf<String>()
                snapshotFlow { currentAvatarUsernames }.collect { usernames ->
                    if (map.style == null) return@collect
                    (uploaded - usernames).forEach { username ->
                        map.removeStyleImage("avatar-$username")
                        uploaded.remove(username)
                    }
                    val missing = usernames - uploaded
                    // Keep the existing bounded bitmap cache; do not retain a second bitmap
                    // for every marker. Batches also release Main between groups of uploads.
                    missing.chunked(AVATAR_UPLOAD_BATCH_SIZE).forEach { batch ->
                        val images = withContext(Dispatchers.Default) {
                            batch.associateWith { username ->
                                createAvatarBitmap(username, colorForUsername(username), sizePx = 150)
                            }
                        }
                        val style = map.style ?: return@collect
                        images.forEach { (username, bitmap) ->
                            style.addImage("avatar-$username", bitmap)
                                .onError { Timber.tag("MAP_UI").w("Unable to add friend avatar: %s", it) }
                                .onValue { uploaded += username }
                        }
                    }
                }
            }
    }

    SymbolLayer(
        sourceState = sourceState,
        layerId = FRIENDS_RING_LAYER_ID,
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

    UpdateFriendsGeoJsonSource(displayedFriends = displayedFriends, sourceState = sourceState)
}

/**
 * Observe animation snapshots without recomposing the layers. Build only changed features on
 * a worker, then submit immutable batches to Mapbox; resetting all data each frame would also
 * discard queued partial updates. A new style gets a fresh collection before incremental updates.
 */
@OptIn(MapboxExperimental::class)
@Composable
private fun UpdateFriendsGeoJsonSource(
    displayedFriends: SnapshotStateMap<String, AnimatedFriendData>,
    sourceState: GeoJsonSourceState,
) {
    MapEffect(sourceState) { mapView ->
        val map = mapView.mapboxMap
        map.styleLoadedEvents.map { Unit }
            .onStart { if (map.style != null) emit(Unit) }
            .collectLatest {
                val source = map.getSourceAs<GeoJsonSource>(sourceState.sourceId)
                    ?: return@collectLatest
                val features = FriendFeatureUpdates()
                var initialized = false
                snapshotFlow { displayedFriends.toMap() }
                    .conflate()
                    .map(features::update)
                    .flowOn(Dispatchers.Default)
                    .collect { changes ->
                        if (!initialized) {
                            source.featureCollection(FeatureCollection.fromFeatures(changes.added))
                            initialized = true
                        } else {
                            if (changes.removed.isNotEmpty()) source.removeGeoJSONSourceFeatures(changes.removed)
                            if (changes.added.isNotEmpty()) source.addGeoJSONSourceFeatures(changes.added)
                            if (changes.updated.isNotEmpty()) source.updateGeoJSONSourceFeatures(changes.updated)
                        }
                    }
            }
    }
}
