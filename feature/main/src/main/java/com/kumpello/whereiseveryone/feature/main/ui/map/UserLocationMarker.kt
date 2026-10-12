package com.kumpello.whereiseveryone.feature.main.ui.map

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.coroutine.styleLoadedEvents
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.ImageValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconPitchAlignmentValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconRotationAlignmentValue
import com.mapbox.maps.extension.compose.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.compose.style.rememberStyleImage
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.updateGeoJSONSourceFeatures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private const val USER_MARKER_FEATURE_ID = "user-location"

@OptIn(MapboxExperimental::class, MapboxDelicateApi::class)
@Composable
internal fun UserLocationMarker(avatar: Bitmap, bearing: Bitmap) {
    val sourceState = remember { GeoJsonSourceState() }
    val avatarImage = rememberStyleImage("user-location-avatar", avatar.asImageBitmap())
    val bearingImage = rememberStyleImage("user-location-bearing", bearing.asImageBitmap())

    // Symbol layers avoid the native 2D puck's zoom/pitch clipping on Vulkan.
    SymbolLayer(sourceState = sourceState, layerId = "user-location-bearing-layer") {
        iconImage = ImageValue(bearingImage)
        iconSize = DoubleValue(1.0)
        iconRotate = DoubleValue(Expression.get("bearing"))
        iconRotationAlignment = IconRotationAlignmentValue.MAP
        iconPitchAlignment = IconPitchAlignmentValue.MAP
        iconAllowOverlap = BooleanValue(true)
        iconIgnorePlacement = BooleanValue(true)
        iconOcclusionOpacity = DoubleValue(1.0)
        iconEmissiveStrength = DoubleValue(1.0)
    }
    SymbolLayer(sourceState = sourceState, layerId = "user-location-avatar-layer") {
        iconImage = ImageValue(avatarImage)
        iconSize = DoubleValue(1.0)
        iconRotationAlignment = IconRotationAlignmentValue.VIEWPORT
        iconPitchAlignment = IconPitchAlignmentValue.MAP
        iconAllowOverlap = BooleanValue(true)
        iconIgnorePlacement = BooleanValue(true)
        iconOcclusionOpacity = DoubleValue(1.0)
        iconEmissiveStrength = DoubleValue(1.0)
    }

    MapEffect(sourceState) { mapView ->
        val map = mapView.mapboxMap
        map.styleLoadedEvents.map { Unit }
            .onStart { if (map.style != null) emit(Unit) }
            .collectLatest {
                val source = map.style?.getSourceAs<GeoJsonSource>(sourceState.sourceId)
                    ?: return@collectLatest
                var initialized = false
                mapView.location.userMarkerFeatures().conflate().collect { feature ->
                    if (initialized) {
                        source.updateGeoJSONSourceFeatures(listOf(feature))
                    } else {
                        source.featureCollection(FeatureCollection.fromFeature(feature))
                        initialized = true
                    }
                }
            }
    }
}

/** Shares the SDK's animated position and heading without recomposing the map each frame. */
internal fun LocationComponentPlugin.userMarkerFeatures(): Flow<Feature> = callbackFlow {
    var position: Point? = null
    var bearing = 0.0

    fun emitMarker() {
        val point = position ?: return
        trySend(Feature.fromGeometry(point, null, USER_MARKER_FEATURE_ID).apply {
            addNumberProperty("bearing", bearing)
        })
    }

    val positionListener = OnIndicatorPositionChangedListener { point ->
        if (point != position) {
            position = point
            emitMarker()
        }
    }
    val bearingListener = OnIndicatorBearingChangedListener { heading ->
        if (heading != bearing) {
            bearing = heading
            emitMarker()
        }
    }
    addOnIndicatorBearingChangedListener(bearingListener)
    addOnIndicatorPositionChangedListener(positionListener)
    awaitClose {
        removeOnIndicatorPositionChangedListener(positionListener)
        removeOnIndicatorBearingChangedListener(bearingListener)
    }
}
