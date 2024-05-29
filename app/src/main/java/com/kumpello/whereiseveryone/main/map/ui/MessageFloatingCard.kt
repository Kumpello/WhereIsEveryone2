package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.domain.model.Location
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow


@Composable
fun MessageFloatingCard(
    modifier: Modifier = Modifier,
    viewState: MapViewModel.ViewState,
    actions: SharedFlow<MapViewModel.Action>,
    trigger: (MapViewModel.Command) -> Unit,
) {
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
                    TextField( //TODO: Make it more funky, maybe other composable? To consider in all Textfields
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Your message") },
                        value = viewState.userMessageField,
                        onValueChange = { message ->
                            trigger(MapViewModel.Command.WriteMessage(message))
                        }
                    )
                    Spacer(Modifier.size(20.dp))
                    Button.Animated(
                        text = stringResource(R.string.update_message),
                        width = 250
                    ) {
                        trigger(MapViewModel.Command.SendMessage)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessagePreview() { //TODO: Get this preview unfucked
    WhereIsEveryoneTheme(darkTheme = true) {
        MessageFloatingCard(
            viewState = MapViewModel.ViewState(
                screenState = ScreenState.Message,
                mapSettings = MapSettings(
                    zoom = 14.15,
                    zoomLocked = false,
                    bearing = 16.17
                ),
                user = Location(
                    lat = 18.19,
                    lon = 20.21,
                    bearing = 22.23f,
                    alt = 24.25,
                    accuracy = 26.27f
                ),
                friends = listOf(),
                userMessage = "Where is everyone?",
                userMessageField = ""

            ), actions = MutableSharedFlow(), trigger = {}
        )
    }
}