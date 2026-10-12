package com.kumpello.whereiseveryone.feature.main.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.components.AppDialog
import com.kumpello.whereiseveryone.core.ui.components.Button
import com.kumpello.whereiseveryone.core.ui.theme.AppElevation
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.core.util.formatDistance
import com.kumpello.whereiseveryone.feature.main.R
import com.kumpello.whereiseveryone.data.repository.RoadInterceptionEstimate
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

@Composable
internal fun CarSuggestionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Text(stringResource(R.string.car_suggestion_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.car_suggestion_description), style = MaterialTheme.typography.bodyLarge)
            Button.Primary(text = stringResource(R.string.enable_car_mode), onClick = onConfirm)
            Button.Secondary(text = stringResource(R.string.not_now), onClick = onDismiss)
        }
    }
}

@Composable
internal fun DrivingSpeedCard(speedKmh: Double?, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = AppElevation.floating,
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppSize.icon))
                Text(stringResource(R.string.car_mode), style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text = speedKmh?.let { String.format(locale, "%.0f", it) } ?: "—",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.kilometers_per_hour), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun CarTrackingDetails(info: CarTrackingInfo) {
    val locale = LocalConfiguration.current.locales[0]
    if (info.isLocationStale) {
        Text(stringResource(R.string.car_tracking_stale), style = MaterialTheme.typography.bodySmall)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        val speed = info.friendSpeedKmh?.let { String.format(locale, "%.0f", it) } ?: "—"
        val difference = info.speedDifferenceKmh?.let { String.format(locale, "%+.0f", it) } ?: "—"
        Text(stringResource(R.string.driver_speed_format, speed), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.speed_difference_format, difference), style = MaterialTheme.typography.bodySmall)
        if (info.isPredictive) {
            InterceptionDetails(info.interceptionEstimate)
        } else {
            val duration = info.travelSeconds?.let { formatTravelTime(it) }
            val distance = info.routeDistanceMeters?.let(::formatDistance)
            Text(
                text = if (duration == null) stringResource(R.string.road_estimate_unavailable)
                else stringResource(R.string.drive_to_latest_location_format, listOfNotNull(duration, distance).joinToString(" • ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InterceptionDetails(estimate: RoadInterceptionEstimate?) {
    val text = when (estimate) {
        is RoadInterceptionEstimate.Intercept -> if (estimate.seconds == 0.0) stringResource(R.string.interception_together)
            else stringResource(R.string.interception_time_format, "${formatTravelTime(estimate.seconds)} • ${formatDistance(estimate.distanceMeters)}")
        is RoadInterceptionEstimate.ArriveLater -> stringResource(
            R.string.arrival_later_time_format,
            "${formatTravelTime(estimate.arrivalSeconds)} • ${formatDistance(estimate.distanceMeters)}",
        )
        RoadInterceptionEstimate.NotReachableAtCurrentSpeed -> stringResource(R.string.interception_not_moving)
        else -> stringResource(R.string.interception_unavailable)
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (estimate is RoadInterceptionEstimate.ArriveLater) {
        Text(
            stringResource(R.string.arrival_late_by_format, formatTravelTime(estimate.secondsAfterDriver)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (estimate is RoadInterceptionEstimate.Intercept || estimate is RoadInterceptionEstimate.ArriveLater) {
        Text(stringResource(R.string.interception_assumption), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun formatTravelTime(seconds: Double): String {
    if (seconds < 60) return stringResource(R.string.eta_seconds_format, ceil(seconds).toInt().coerceAtLeast(1))
    val minutes = ceil(seconds / 60.0).toInt()
    return if (minutes < 60) stringResource(R.string.eta_minutes_format, minutes)
    else stringResource(R.string.eta_hours_minutes_format, minutes / 60, minutes % 60)
}

@Preview(showBackground = true)
@Composable
private fun DrivingSpeedPreview() {
    WhereIsEveryoneTheme { DrivingSpeedCard(72.0) }
}
