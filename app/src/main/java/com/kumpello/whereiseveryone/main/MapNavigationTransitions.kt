package com.kumpello.whereiseveryone.main

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.RoundedCornerCompat
import androidx.core.view.ViewCompat
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.feature.main.navigation.MainRoute

@Composable
internal fun rememberMapNavigationTransitions(): MapNavigationTransitions {
    val windowSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing
    val topInset = insets.getTop(density)
    val leftInset = insets.getLeft(density, layoutDirection)
    val rightInset = insets.getRight(density, layoutDirection)

    return remember(windowSize, density, layoutDirection, topInset, leftInset, rightInset) {
        // Restored screens and deep links can animate before the map has measured its icons.
        val edgeToCenter = with(density) {
            (AppSpacing.md + AppSpacing.xxs + AppSize.touchTarget / 2).toPx()
        }
        val buttonStep = with(density) { (AppSize.touchTarget + AppSpacing.xxs).toPx() }
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val settingsCenter = Offset(
            x = if (isRtl) leftInset + edgeToCenter else windowSize.width - rightInset - edgeToCenter,
            y = topInset + edgeToCenter
        )
        MapNavigationTransitions(
            containerBounds = Rect(0f, 0f, windowSize.width.toFloat(), windowSize.height.toFloat()),
            friendsIconCenter = settingsCenter + Offset(if (isRtl) buttonStep else -buttonStep, 0f),
            settingsIconCenter = settingsCenter
        )
    }
}

@Composable
internal fun rememberDisplayCornerShape(): Shape {
    val view = LocalView.current
    var shape by remember(view) { mutableStateOf(readDisplayCornerShape(view)) }

    DisposableEffect(view) {
        // Observe layout without replacing Compose's window-insets listener.
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            shape = readDisplayCornerShape(view)
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        shape = readDisplayCornerShape(view)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return shape
}

private fun readDisplayCornerShape(view: View): AbsoluteRoundedCornerShape {
    val insets = ViewCompat.getRootWindowInsets(view)
    // Pixel sizes match the physical corners and must not mirror in RTL layouts.
    return AbsoluteRoundedCornerShape(
        topLeft = (insets?.getRoundedCorner(RoundedCornerCompat.POSITION_TOP_LEFT)?.radius ?: 0).toFloat(),
        topRight = (insets?.getRoundedCorner(RoundedCornerCompat.POSITION_TOP_RIGHT)?.radius ?: 0).toFloat(),
        bottomRight = (insets?.getRoundedCorner(RoundedCornerCompat.POSITION_BOTTOM_RIGHT)?.radius ?: 0).toFloat(),
        bottomLeft = (insets?.getRoundedCorner(RoundedCornerCompat.POSITION_BOTTOM_LEFT)?.radius ?: 0).toFloat()
    )
}

internal class MapNavigationTransitions(
    containerBounds: Rect,
    friendsIconCenter: Offset,
    settingsIconCenter: Offset
) {
    private var bounds by mutableStateOf(containerBounds)
    private var friendsCenter by mutableStateOf(friendsIconCenter)
    private var settingsCenter by mutableStateOf(settingsIconCenter)

    fun updateBounds(containerBounds: Rect) {
        bounds = containerBounds
    }

    fun updateIconCenter(route: MainRoute, center: Offset) {
        when (route) {
            MainRoute.Friends -> friendsCenter = center
            MainRoute.Settings -> settingsCenter = center
            MainRoute.Map -> Unit
        }
    }

    fun enter(destination: NavDestination): EnterTransition {
        val origin = transformOrigin(destination) ?: return EnterTransition.None
        return scaleIn(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            initialScale = 0f,
            transformOrigin = origin
        ) + fadeIn(animationSpec = tween(durationMillis = 90))
    }

    fun exit(destination: NavDestination): ExitTransition {
        val origin = transformOrigin(destination) ?: return ExitTransition.None
        return scaleOut(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            targetScale = 0f,
            transformOrigin = origin
        ) + fadeOut(animationSpec = tween(durationMillis = 90, delayMillis = 210))
    }

    private fun transformOrigin(destination: NavDestination): TransformOrigin? {
        val center = when {
            destination.hasRoute<MainRoute.Friends>() -> friendsCenter
            destination.hasRoute<MainRoute.Settings>() -> settingsCenter
            else -> return null
        }
        if (bounds.width <= 0f || bounds.height <= 0f) return TransformOrigin.Center
        return TransformOrigin(
            pivotFractionX = ((center.x - bounds.left) / bounds.width).coerceIn(0f, 1f),
            pivotFractionY = ((center.y - bounds.top) / bounds.height).coerceIn(0f, 1f)
        )
    }
}
