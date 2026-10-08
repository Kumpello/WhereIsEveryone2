package com.kumpello.whereiseveryone.main.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.AppDialog
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.AppSize
import com.kumpello.whereiseveryone.common.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.util.LocationUtils

@Composable
fun FriendDetailsCard(
    friend: Friend,
    onDismiss: () -> Unit,
    onNavigate: (Friend) -> Unit,
    onSharingToggle: (Friend) -> Unit
) {
    AppDialog(onDismiss = onDismiss) {
        FriendDetailsContent(friend, onDismiss, onNavigate, onSharingToggle)
    }
}

@Composable
private fun FriendDetailsContent(
    friend: Friend,
    onDismiss: () -> Unit,
    onNavigate: (Friend) -> Unit,
    onSharingToggle: (Friend) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(
            friend.username,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        if (friend.status.isNotBlank()) {
            Text(friend.status, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        friend.formattedDistance?.let {
            Text(stringResource(R.string.distance_format, it), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            friend.location?.let { loc ->
                DetailItem(label = stringResource(R.string.latitude_label), value = loc.lat.toString())
                DetailItem(label = stringResource(R.string.longitude_label), value = loc.lon.toString())
                loc.bearing?.let {
                    DetailItem(label = stringResource(R.string.bearing_label), value = "$it°")
                }
                loc.rawAlt?.let {
                    DetailItem(
                        label = stringResource(R.string.altitude_label),
                        value = "${loc.alt.displayName} (${it.toInt()}m)"
                    )
                }
                loc.rawAccuracy?.let {
                    DetailItem(
                        label = stringResource(R.string.accuracy_label),
                        value = "${loc.accuracy.displayName} (${String.format(locale, "%.2f", it)}m)"
                    )
                }
                LocationUtils.formatSpeed(loc.speed)?.let {
                    DetailItem(label = stringResource(R.string.speed_label), value = it)
                }
                DetailItem(label = stringResource(R.string.last_update_label), value = loc.lastUpdateTime)
                DetailItem(
                    label = stringResource(R.string.data_age_label),
                    value = loc.lastUpdateAge.displayName
                )
            }
            friend.friendSince?.let {
                DetailItem(label = stringResource(R.string.friend_since_label), value = it)
            }
        }
        Button.Secondary(text = stringResource(if (friend.isPaused) R.string.resume_sharing else R.string.stop_sharing)) {
            onSharingToggle(friend)
        }
        Button.Primary(text = stringResource(R.string.navigate_action)) { onNavigate(friend) }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = AppSize.touchTarget)) {
            Text(stringResource(R.string.close))
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Text(
            label,
            modifier = Modifier.weight(0.45f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, modifier = Modifier.weight(0.55f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

@Preview(showBackground = true)
@Composable
fun FriendDetailsPreview() {
    WhereIsEveryoneTheme {
        FriendDetailsContent(
            friend = Friend(
                username = "JanuszAndrzejNowak",
                status = "Doing something cool",
                state = FriendState.ACCEPTED,
                formattedDistance = "1.3km",
                location = Location(
                    lat = 52.2297,
                    lon = 21.0122,
                    bearing = 45.0f,
                    alt = AltDifference.SOMEWHAT_SAME,
                    rawAlt = 100.0,
                    accuracy = AccuracyLevel.HIGH,
                    rawAccuracy = 5.0f,
                    speed = 10.0f,
                    lastUpdateTime = "12:34:56 03.06.2026",
                    lastUpdateAge = LastUpdateAge.FRESH
                ),
                friendSince = "01.01.2024"
            ),
            onDismiss = {},
            onNavigate = {},
            onSharingToggle = {}
        )
    }
}
