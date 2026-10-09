package com.kumpello.whereiseveryone.feature.main.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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

    // The native scale retains its distance calculations, locale units and on-demand redraws.
    ScaleBar(
        modifier = Modifier
            .safeDrawingPadding()
            .padding(
                start = AppSpacing.md,
                bottom = AppSize.mapControl + AppSpacing.lg,
            )
            .shadow(AppElevation.floating, shape)
            .background(colors.surfaceContainerHigh, shape)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(AppSpacing.xs),
        contentPadding = PaddingValues(0.dp),
        alignment = Alignment.BottomStart,
        textColor = colors.onSurface,
        primaryColor = colors.primary,
        secondaryColor = colors.surfaceContainerHigh,
        borderWidth = 1.dp,
        height = AppSpacing.xxs,
        textBarMargin = AppSpacing.xxs,
        textSize = MaterialTheme.typography.labelMedium.fontSize,
        showTextBorder = false,
        ratio = 0.3f,
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
