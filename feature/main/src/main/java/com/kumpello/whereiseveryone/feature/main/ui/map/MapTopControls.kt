package com.kumpello.whereiseveryone.feature.main.ui.map

import com.kumpello.whereiseveryone.feature.main.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.feature.main.navigation.MainRoute

@Composable
fun MapTopControls(
    modifier: Modifier = Modifier,
    incomingFriendRequestCount: Int = 0,
    onNavigationIconPositioned: (MainRoute, Offset) -> Unit = { _, _ -> },
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
            contentDescription = if (incomingFriendRequestCount > 0) {
                pluralStringResource(R.plurals.friends_pending_requests_cd, incomingFriendRequestCount, incomingFriendRequestCount)
            } else {
                stringResource(R.string.friends_cd)
            },
            badgeCount = incomingFriendRequestCount,
            modifier = Modifier.onGloballyPositioned {
                onNavigationIconPositioned(
                    MainRoute.Friends(),
                    it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                )
            },
            onClick = { onEvent(MapScreenViewModel.Event.NavigateFriends) }
        )
        ControlButton(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.settings_cd),
            modifier = Modifier.onGloballyPositioned {
                onNavigationIconPositioned(
                    MainRoute.Settings,
                    it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                )
            },
            onClick = { onEvent(MapScreenViewModel.Event.NavigateSettings) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapTopControlsPreview() {
    WhereIsEveryoneTheme {
        MapTopControls(incomingFriendRequestCount = 3, onEvent = {})
    }
}
