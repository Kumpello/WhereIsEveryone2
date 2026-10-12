package com.kumpello.whereiseveryone.feature.main.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.core.ui.theme.AppElevation
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.feature.main.R
import com.mapbox.maps.extension.compose.ornaments.compass.MapCompassScope
import com.mapbox.maps.extension.compose.ornaments.scalebar.MapScaleBarScope

@Composable
internal fun MapCompassScope.MapCompass() {
    // Keep Mapbox's bearing tracking, north-facing fade and camera reset behavior.
    Compass(
        modifier = Modifier.safeDrawingPadding().padding(AppSpacing.md),
        contentPadding = PaddingValues(0.dp),
        alignment = Alignment.BottomStart,
        content = { MapCompassFace() },
    )
}

@Composable
internal fun MapScaleBarScope.MapScaleBar() {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val horizontalInsets = with(density) {
        (WindowInsets.safeDrawing.getLeft(density, layoutDirection) +
            WindowInsets.safeDrawing.getRight(density, layoutDirection)).toDp()
    }
    val startPadding = AppSpacing.md + AppSize.mapControl + AppSpacing.xs
    // Three buttons, two gaps and the ControlBar's padding on both sides.
    val bottomControlsWidth = AppSize.touchTarget * 3 + AppSpacing.xxs * 4
    val endPadding = AppSpacing.md + bottomControlsWidth + AppSpacing.xs
    // Mapbox adds 10dp internally for labels, in addition to our surface padding.
    val scaleWidth = windowWidth - horizontalInsets - startPadding - endPadding -
        AppSpacing.xs * 2 - 10.dp
    if (scaleWidth <= 0.dp || windowWidth <= 0.dp) return

    // The native scale retains its distance calculations, locale units and on-demand redraws.
    ScaleBar(
        modifier = Modifier
            .safeDrawingPadding()
            .padding(
                start = startPadding,
                end = endPadding,
                bottom = AppSpacing.md,
            )
            .shadow(AppElevation.floating, shape)
            .background(colors.surfaceContainerHigh, shape)
            .border(1.dp, colors.outlineVariant, shape)
            .heightIn(min = AppSize.mapControl)
            .padding(AppSpacing.xs)
            .wrapContentHeight(Alignment.CenterVertically),
        contentPadding = PaddingValues(0.dp),
        alignment = Alignment.BottomStart,
        textColor = colors.onSurface,
        primaryColor = colors.primary,
        secondaryColor = colors.surfaceContainerHigh,
        borderWidth = 1.dp,
        height = AppSpacing.sm,
        textBarMargin = AppSpacing.xs,
        textSize = MaterialTheme.typography.bodyLarge.fontSize,
        showTextBorder = false,
        ratio = (scaleWidth / windowWidth).coerceAtMost(0.3f),
        useContinuousRendering = false,
    )
}

@Composable
private fun MapCompassFace() {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.map_compass_cd)

    // A circular surface can rotate with the SDK ornament without rotating a square panel.
    Surface(
        modifier = Modifier.size(AppSize.mapControl),
        shape = CircleShape,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = AppElevation.floating,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Spacer(
                modifier = Modifier
                    .size(AppSpacing.xl)
                    .semantics { contentDescription = description }
                    .drawWithCache {
                        val north = Path().apply {
                            moveTo(size.width / 2f, size.height * 0.08f)
                            lineTo(size.width * 0.7f, size.height / 2f)
                            lineTo(size.width * 0.3f, size.height / 2f)
                            close()
                        }
                        val south = Path().apply {
                            moveTo(size.width / 2f, size.height * 0.92f)
                            lineTo(size.width * 0.3f, size.height / 2f)
                            lineTo(size.width * 0.7f, size.height / 2f)
                            close()
                        }
                        val ringStroke = Stroke(1.dp.toPx())

                        onDrawBehind {
                            drawCircle(
                                color = colors.outlineVariant,
                                radius = size.minDimension / 2f - ringStroke.width / 2f,
                                style = ringStroke,
                            )
                            drawPath(north, colors.primary)
                            drawPath(south, colors.onSurfaceVariant)
                        }
                    },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapCompassLightPreview() {
    WhereIsEveryoneTheme(darkTheme = false) { MapCompassFace() }
}

@Preview(showBackground = true, backgroundColor = 0xFF050805)
@Composable
private fun MapCompassDarkPreview() {
    WhereIsEveryoneTheme(darkTheme = true) { MapCompassFace() }
}
