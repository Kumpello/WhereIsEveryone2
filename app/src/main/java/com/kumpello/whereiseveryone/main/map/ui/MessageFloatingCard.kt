package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.common.ui.TextField
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.ui.FloatingCard
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel


@Composable
fun MessageFloatingCard(
    modifier: Modifier = Modifier,
    viewState: MapViewModel.ViewState,
    trigger: (MapViewModel.Event) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        FloatingCard(modifier = modifier) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = Shapes.large,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = "Current status message:") //TODO: Customize style
                        Text(text = viewState.userMessage) //TODO: Doesn't look good
                        TextField.Regular(
                            label = stringResource(R.string.your_message),
                            value = viewState.userMessageField,
                            onValueChange = { message ->
                                trigger(MapViewModel.Event.WriteMessage(message))
                            }
                        )
                        Spacer(Modifier.size(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button.Animated(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.clear_message),
                            ) {
                                trigger(MapViewModel.Event.ClearMessage)
                            }
                            Button.Animated(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.update_message),
                            ) {
                                trigger(MapViewModel.Event.SendMessage)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessagePreview() {
    WhereIsEveryoneTheme(darkTheme = true) {
        MessageFloatingCard(
            viewState = MapViewModel.ViewState(
                screenState = ScreenState.Message,
                mapSettings = MapSettings(
                    zoom = 14.15,
                    zoomLocked = false,
                ),
                user = Location(
                    lat = 0.0,
                    lon = 0.0,
                    bearing = 0.0f,
                    alt = AltDifference.WAY_HIGHER,
                    accuracy = AccuracyLevel.PERFECT,
                    lastUpdateTime = "20.04.2137",
                    lastUpdateAge = LastUpdateAge.FRESH,
                    rawAlt = 0.0,
                    rawAccuracy = 0f,
                ),
                friends = listOf(),
                userMessage = "Where is everyone?",
                userMessageField = "",
                permissions = emptyMap(),
                showPermissionNotification = false,
                selectedFriend = null,
                navigatingFriend = null,
                bearingToFriend = null
            ), trigger = {}, onDismiss = {}
        )
    }
}
