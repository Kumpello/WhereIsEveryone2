package com.kumpello.whereiseveryone.main.common.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailItem(label = "Latitude", value = friend.location.lat.toString())
                DetailItem(label = "Longitude", value = friend.location.lon.toString())
                DetailItem(label = "Bearing", value = friend.location.bearing?.let { "$it°" } ?: "Unknown")
                DetailItem(
                    label = "Altitude",
                    value = "${friend.location.alt.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} (${friend.location.rawAlt ?: "N/A"}m)"
                )
                DetailItem(
                    label = "Accuracy",
                    value = "${friend.location.accuracy.name.lowercase().replaceFirstChar { it.uppercase() }} (${friend.location.rawAccuracy ?: "N/A"}m)"
                )
                DetailItem(label = "Last Update", value = friend.location.lastUpdateTime)
                DetailItem(label = "Data Age", value = friend.location.lastUpdateAge.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })

                Spacer(modifier = Modifier.size(16.dp))
                Button.Animated(
                    text = "Close",
                    width = 200
                ) {
                    onDismiss()
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
        FriendDetailsCard(
            friend = Friend(
                username = "JanuszAndrzejNowak",
                status = "Doing something cool",
                state = FriendState.ACCEPTED,
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
                )
            ),
            onDismiss = {}
        )
    }
}
