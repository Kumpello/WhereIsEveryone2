package com.kumpello.whereiseveryone.feature.main.ui.map

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.feature.main.R
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun NavigationCompass(
    modifier: Modifier = Modifier,
    bearing: Float,
    friendName: String,
    distance: String?,
    onCancel: () -> Unit,
    carTrackingInfo: CarTrackingInfo? = null,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier.widthIn(max = AppSize.formMaxWidth).fillMaxWidth()
            .zIndex(1001f)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX.value < -100f) {
                            onCancel()
                        } else {
                            scope.launch {
                                offsetX.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + dragAmount).coerceAtMost(0f))
                        }
                    }
                )
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(R.string.direction_to_format, friendName),
                modifier = Modifier
                    .size(32.dp)
                    .rotate(bearing),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friendName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = listOfNotNull("${bearing.toInt()}°", distance).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                carTrackingInfo?.let { CarTrackingDetails(it) }
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(AppSize.touchTarget)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_navigation), modifier = Modifier.size(AppSize.icon))
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationCompassPreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavigationCompass(
                modifier = Modifier.padding(AppSpacing.md),
                bearing = 45f,
                friendName = "Anna",
                distance = "850 m",
                onCancel = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationCompassDrivingPreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavigationCompass(
                modifier = Modifier.padding(AppSpacing.md),
                bearing = 135f,
                friendName = "Anna",
                distance = "7.2 km",
                onCancel = {},
                carTrackingInfo = CarTrackingInfo(
                    friendSpeedKmh = 72.0,
                    speedDifferenceKmh = -8.0,
                    travelSeconds = 360.0,
                    routeDistanceMeters = 7200.0,
                    isLocationStale = false
                )
            )
        }
    }
}
