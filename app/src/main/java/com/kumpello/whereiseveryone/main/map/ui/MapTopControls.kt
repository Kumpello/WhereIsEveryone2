package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.map.presentation.MapScreenViewModel

@Composable
fun MapTopControls(
    modifier: Modifier = Modifier,
    onEvent: (MapScreenViewModel.Event) -> Unit
) {
    ControlBar(modifier = modifier) {
        ControlButton(
            imageVector = Icons.Default.Edit,
            contentDescription = stringResource(R.string.message_cd),
            onClick = { onEvent(MapScreenViewModel.Event.NavigateMessage) }
        )
        ControlButton(
            imageVector = Icons.Default.LocationOn,
            contentDescription = stringResource(R.string.find_me_cd),
            onClick = { onEvent(MapScreenViewModel.Event.FindMeClicked) }
        )
        ControlButton(
            imageVector = Icons.Default.Person,
            contentDescription = stringResource(R.string.friends_cd),
            onClick = { onEvent(MapScreenViewModel.Event.NavigateFriends) }
        )
        ControlButton(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.settings_cd),
            onClick = { onEvent(MapScreenViewModel.Event.NavigateSettings) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapTopControlsPreview() {
    WhereIsEveryoneTheme {
        MapTopControls(onEvent = {})
    }
}
