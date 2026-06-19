package com.kumpello.whereiseveryone.main.map.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.MainRoute
import com.kumpello.whereiseveryone.main.common.ui.Notification
import com.kumpello.whereiseveryone.main.map.entity.MapSettings
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun MapScreen( //TODO: Add compass pointing to friend, add friends nearby
    navController: NavController,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                MapViewModel.Action.NavigateFriends -> navController.navigate(MainRoute.Friends)
                MapViewModel.Action.NavigateSettings -> navController.navigate(MainRoute.Settings)
                is MapViewModel.Action.Toast -> Toast.makeText(
                    context,
                    action.id,
                    Toast.LENGTH_SHORT
                )
                else -> Unit
            }
        }
    }

    BackHandler(state.screenState != ScreenState.Map) {
        viewModel.trigger(MapViewModel.Event.BackToMap)
    }

    MapScreen(
        viewState = state,
        action = viewModel.action,
        event = viewModel::trigger
    )
}

@Composable
fun MapScreen(
    viewState: MapViewModel.ViewState,
    action: Flow<MapViewModel.Action>,
    event: (MapViewModel.Event) -> Unit,
) {
    Box {
        if (viewState.showPermissionNotification) {
            Notification(
                modifier = Modifier
                    .zIndex(Float.MAX_VALUE)
                    .align(Alignment.Center)
                    .padding(8.dp),
                notification = stringResource(R.string.permissions_message),
                onAllowClick = { event(MapViewModel.Event.OnPermissionAllow) },
                onDenyClick = { event(MapViewModel.Event.OnPermissionDeny) }
            )
        }
        TopControls(
            modifier = Modifier.align(Alignment.TopEnd),
            onEvent = event
        )
        MapContent(
            modifier = Modifier.align(Alignment.Center),
            state = viewState.mapSettings,
            actions = action,
            userLocation = viewState.user,
            friendsPositions = viewState.friends,
            event = event
        )
        when (viewState.screenState) {
            is ScreenState.Message -> MessageFloatingCard(
                modifier = Modifier
                    .padding(top = 128.dp)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.9f),
                viewState = viewState,
                onDismiss = { event(MapViewModel.Event.BackToMap) },
                trigger = event
            )

            else -> Unit
        }
        BottomControls(
            modifier = Modifier.align(Alignment.BottomEnd),
            onEvent = event
        )
    }
}

@Composable
private fun TopControls(
    modifier: Modifier = Modifier,
    onEvent: (MapViewModel.Event) -> Unit
) {
    ControlBar(modifier = modifier) {
        ControlButton(
            imageVector = Icons.Default.Edit,
            contentDescription = "Message",
            onClick = { onEvent(MapViewModel.Event.NavigateMessage) }
        )
        ControlButton(
            imageVector = Icons.Default.Person,
            contentDescription = "Friends",
            onClick = { onEvent(MapViewModel.Event.NavigateFriends) }
        )
        ControlButton(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = { onEvent(MapViewModel.Event.NavigateSettings) }
        )
    }
}

@Composable
private fun BottomControls(
    modifier: Modifier = Modifier,
    onEvent: (MapViewModel.Event) -> Unit
) {
    ControlBar(modifier = modifier) {
        ControlButton(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Zoom out",
            onClick = { onEvent(MapViewModel.Event.ZoomOut) }
        )
        ControlButton(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Zoom in",
            onClick = { onEvent(MapViewModel.Event.ZoomIn) }
        )
        ControlButton(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Center",
            onClick = { onEvent(MapViewModel.Event.CenterMap) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    WhereIsEveryoneTheme {
        MapScreen(
            viewState = MapViewModel.ViewState(
                showPermissionNotification = false,
                permissions = emptyMap(),
                screenState = ScreenState.Map,
                mapSettings = MapSettings(),
                user = null,
                friends = emptyList(),
                userMessage = "Status message",
                userMessageField = ""
            ),
            action = emptyFlow(),
            event = {}
        )
    }
}
