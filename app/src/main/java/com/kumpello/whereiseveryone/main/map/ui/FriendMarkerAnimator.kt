package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// --- Friend marker animation -------------------------------------------------------------
//
// Every field stays a Double from start to finish. Compose's `animate()` only knows how to
// interpolate Float-backed types (Float, AnimationVector1D/2D/3D/4D, etc.) — there's no
// Double-precision animatable. Feeding lat/lon through one of those (even via a custom
// TwoWayConverter) truncates them to ~7 significant digits of Float precision, which is
// visible as jitter once zoomed in on a marker (lat/lon need ~8 digits for meter-level
// accuracy). So instead we animate a plain Float "progress" 0f -> 1f — which needs almost no
// precision — let Compose apply the tween's easing curve to that, and do the actual
// interpolation of every Double field ourselves. One simple technique, used uniformly, with
// no custom converters or AnimationVector types anywhere in this file.
//
// Fields are still grouped into separate LaunchedEffects so a change in one (e.g. opacity
// fading as an update ages) can never cancel/restart another (e.g. an in-flight position
// tween) — each restarts only when its own target actually changes.

private const val FRIEND_ANIMATION_DURATION_MS = 1000

private fun shortestBearingTarget(current: Double, target: Double): Double {
    var delta = (target - current) % 360.0
    if (delta > 180.0) delta -= 360.0
    if (delta < -180.0) delta += 360.0
    return current + delta
}

private fun normalizeBearing(bearing: Double): Double = ((bearing % 360.0) + 360.0) % 360.0

private fun lerp(from: Double, to: Double, progress: Double): Double = from + (to - from) * progress

/** Runs an eased 0f->1f tween and reports Double progress each frame — the only animation primitive this file needs. */
private suspend fun animateProgress(
    durationMs: Int = FRIEND_ANIMATION_DURATION_MS,
    onFrame: (progress: Double) -> Unit,
) {
    animate(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = tween(durationMillis = durationMs),
    ) { value, _ -> onFrame(value.toDouble()) }
}

@Composable
fun AnimatedFriendEffect(
    id: String,
    target: AnimatedFriendData,
    onUpdate: (AnimatedFriendData) -> Unit,
    onRemoved: () -> Unit,
) {
    // mutableDoubleStateOf avoids boxing each Double into an object on every frame write.
    var lat by remember { mutableDoubleStateOf(target.lat) }
    var lon by remember { mutableDoubleStateOf(target.lon) }
    var bearing by remember { mutableDoubleStateOf(target.bearing) }
    var haloWidth by remember { mutableDoubleStateOf(target.haloWidth) }
    var speed by remember { mutableDoubleStateOf(target.speed) }
    var opacity by remember { mutableDoubleStateOf(target.opacity) }

    fun currentValue() = AnimatedFriendData(
        lat = lat,
        lon = lon,
        bearing = bearing,
        opacity = opacity,
        haloWidth = haloWidth,
        speed = speed,
    )

    // Show the un-animated starting value immediately so the marker appears right away.
    LaunchedEffect(Unit) {
        onUpdate(currentValue())
    }

    // Position + bearing move together (same reported update, same duration), restarting
    // only when either actually changes.
    LaunchedEffect(target.lat, target.lon, target.bearing) {
        val startLat = lat
        val startLon = lon
        val startBearing = bearing
        val bearingTarget = shortestBearingTarget(startBearing, target.bearing)

        animateProgress { p ->
            lat = lerp(startLat, target.lat, p)
            lon = lerp(startLon, target.lon, p)
            bearing = lerp(startBearing, bearingTarget, p)
            onUpdate(currentValue())
        }
        // Fold the (possibly >360/<0) bearing back into 0..360 once settled.
        bearing = normalizeBearing(bearing)
        onUpdate(currentValue())
    }

    // Halo size + speed, restarting only when either changes.
    LaunchedEffect(target.haloWidth, target.speed) {
        val startHalo = haloWidth
        val startSpeed = speed
        animateProgress { p ->
            haloWidth = lerp(startHalo, target.haloWidth, p)
            speed = lerp(startSpeed, target.speed, p)
            onUpdate(currentValue())
        }
    }

    // Opacity, restarting only when it changes — isolated so its own tween can never
    // disturb position/bearing/size.
    LaunchedEffect(target.opacity) {
        val start = opacity
        animateProgress { p ->
            opacity = lerp(start, target.opacity, p)
            onUpdate(currentValue())
        }
    }

    DisposableEffect(id) {
        onDispose { onRemoved() }
    }
}