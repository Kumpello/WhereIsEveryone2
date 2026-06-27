package com.kumpello.whereiseveryone.main.common.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.Location

@Composable
fun FriendDetailsCard(
    friend: Friend,
    onDismiss: () -> Unit,
    onNavigate: (Friend) -> Unit,
    onSharingToggle: (Friend) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            FriendDetailsContent(
                friend = friend,
                onDismiss = onDismiss,
                onNavigate = onNavigate,
                onSharingToggle = onSharingToggle
            )
        }
    }
}

@Composable
private fun FriendDetailsContent(
    friend: Friend,
    onDismiss: () -> Unit,
    onNavigate: (Friend) -> Unit,
    onSharingToggle: (Friend) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = Shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = friend.username,
                modifier = Modifier.basicMarquee(),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = friend.status,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            friend.formattedDistance?.let {
                Text(
                    text = "Distance: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            friend.location?.let { loc ->
                DetailItem(label = "Latitude", value = loc.lat.toString())
                DetailItem(label = "Longitude", value = loc.lon.toString())
                loc.bearing?.let {
                    DetailItem(label = "Bearing", value = "$it°")
                }
                loc.rawAlt?.let {
                    DetailItem(
                        label = "Altitude",
                        value = "${
                            loc.alt.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() }
                        } (${it}m)"
                    )
                }
                loc.rawAccuracy?.let {
                    DetailItem(
                        label = "Accuracy",
                        value = "${
                            loc.accuracy.name.lowercase().replaceFirstChar { it.uppercase() }
                        } (${it}m)"
                    )
                }
                DetailItem(label = "Last Update", value = loc.lastUpdateTime)
                DetailItem(
                    label = "Data Age",
                    value = loc.lastUpdateAge.name.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() }
                )
            }
            friend.friendSince?.let {
                DetailItem(label = "Friend since", value = it)
            }

            Spacer(modifier = Modifier.size(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button.Animated(
                    text = if (friend.isPaused) stringResource(R.string.resume_sharing) else stringResource(R.string.stop_sharing),
                    width = 268
                ) {
                    onSharingToggle(friend)
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button.Animated(
                    text = "Close",
                    width = 130
                ) {
                    onDismiss()
                }
                Spacer(modifier = Modifier.size(8.dp))
                Button.Animated(
                    text = "Navigate!",
                    width = 130
                ) {
                    onNavigate(friend)
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
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
