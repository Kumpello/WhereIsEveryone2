package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// --- Friend marker animation -------------------------------------------------------------
//
// Each friend's marker is driven by its own animate() coroutine (started/restarted only when
// *that* friend's target changes) instead of one shared withFrameNanos loop for the whole
// list. That means one friend's location update no longer restarts every other friend's
// in-flight animation. The interpolated values are written into a snapshot-backed map that's
// read directly during composition to rebuild the GeoJSON source data, the same way Mapbox's
// own Compose examples assign `GeoJsonSourceState.data` straight from an animated value.

private const val FRIEND_ANIMATION_DURATION_MS = 1000

/** Position + heading + opacity are interpolated together as a 4D vector. */
private data class PositionBearingOpacity(
    val lon: Double,
    val lat: Double,
    val bearing: Double,
    val opacity: Double,
)

/** Halo size + speed are interpolated together as a 2D vector. */
private data class SizeMetrics(
    val haloWidth: Double,
    val speed: Double,
)

private val PositionBearingOpacityConverter = TwoWayConverter<PositionBearingOpacity, AnimationVector4D>(
    convertToVector = {
        AnimationVector4D(it.lon.toFloat(), it.lat.toFloat(), it.bearing.toFloat(), it.opacity.toFloat())
    },
    convertFromVector = {
        PositionBearingOpacity(it.v1.toDouble(), it.v2.toDouble(), it.v3.toDouble(), it.v4.toDouble())
    }
)

private val SizeMetricsConverter = TwoWayConverter<SizeMetrics, AnimationVector2D>(
    convertToVector = { AnimationVector2D(it.haloWidth.toFloat(), it.speed.toFloat()) },
    convertFromVector = { SizeMetrics(it.v1.toDouble(), it.v2.toDouble()) }
)

/**
 * Bearing is circular (350° -> 10° should turn +20°, not -340°). Rather than re-deriving the
 * shortest path every frame, pick an equivalent target angle (possibly outside 0..360) that's
 * angularly closest to [current] once, up front — a plain linear tween to that value then
 * automatically takes the short way around.
 */
private fun shortestBearingTarget(current: Double, target: Double): Double {
    var delta = (target - current) % 360.0
    if (delta > 180.0) delta -= 360.0
    if (delta < -180.0) delta += 360.0
    return current + delta
}

private fun normalizeBearing(bearing: Double): Double = ((bearing % 360.0) + 360.0) % 360.0

/**
 * No UI of its own. Owns and animates the position/bearing/opacity/size state for a single
 * friend and reports every interpolated frame back through [onUpdate]. The caller wraps each
 * instance in `key(id) { ... }`, so this composable's lifetime — and therefore its coroutines —
 * is scoped to that one friend, independent of everyone else's updates.
 */
@Composable
fun AnimatedFriendEffect(
    id: String,
    target: AnimatedFriendData,
    onUpdate: (AnimatedFriendData) -> Unit,
    onRemoved: () -> Unit,
) {
    var positionBearingOpacity by remember {
        mutableStateOf(PositionBearingOpacity(target.lon, target.lat, target.bearing, target.opacity))
    }
    var sizeMetrics by remember {
        mutableStateOf(SizeMetrics(target.haloWidth, target.speed))
    }

    fun currentValue() = AnimatedFriendData(
        lat = positionBearingOpacity.lat,
        lon = positionBearingOpacity.lon,
        bearing = positionBearingOpacity.bearing,
        opacity = positionBearingOpacity.opacity,
        haloWidth = sizeMetrics.haloWidth,
        speed = sizeMetrics.speed,
    )

    // Push the un-animated starting value immediately so the marker shows up right away.
    LaunchedEffect(Unit) {
        onUpdate(currentValue())
    }

    LaunchedEffect(target) {
        val bearingTarget = shortestBearingTarget(positionBearingOpacity.bearing, target.bearing)

        // Two independent launches so a write from one never clobbers the other's in-flight value.
        coroutineScope {
            launch {
                animate(
                    typeConverter = PositionBearingOpacityConverter,
                    initialValue = positionBearingOpacity,
                    targetValue = PositionBearingOpacity(target.lon, target.lat, bearingTarget, target.opacity),
                    animationSpec = tween(durationMillis = FRIEND_ANIMATION_DURATION_MS),
                ) { value, _ ->
                    positionBearingOpacity = value
                    onUpdate(currentValue())
                }
                // Fold the (possibly >360/<0) bearing back into 0..360 once settled.
                positionBearingOpacity =
                    positionBearingOpacity.copy(bearing = normalizeBearing(positionBearingOpacity.bearing))
                onUpdate(currentValue())
            }
            launch {
                animate(
                    typeConverter = SizeMetricsConverter,
                    initialValue = sizeMetrics,
                    targetValue = SizeMetrics(target.haloWidth, target.speed),
                    animationSpec = tween(durationMillis = FRIEND_ANIMATION_DURATION_MS),
                ) { value, _ ->
                    sizeMetrics = value
                    onUpdate(currentValue())
                }
            }
        }
    }

    DisposableEffect(id) {
        onDispose { onRemoved() }
    }
}
