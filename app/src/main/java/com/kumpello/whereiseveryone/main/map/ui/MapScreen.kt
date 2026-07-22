package com.kumpello.whereiseveryone.main.map.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.MainRoute
import com.kumpello.whereiseveryone.main.common.ui.Notification
import com.kumpello.whereiseveryone.main.map.presentation.MapScreenViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapScreen(
    navController: NavController,
    screenViewModel: MapScreenViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val screenState by screenViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        screenViewModel.action.collect { action ->
            when (action) {
                MapScreenViewModel.Action.NavigateFriends -> navController.navigate(MainRoute.Friends)
                MapScreenViewModel.Action.NavigateSettings -> navController.navigate(MainRoute.Settings)
                is MapScreenViewModel.Action.Toast -> Toast.makeText(
                    context,
                    action.id,
                    Toast.LENGTH_SHORT
                ).show()

                is MapScreenViewModel.Action.ShowPermissionSettings -> { /* Handle in Activity */
                }
            }
        }
    }

    BackHandler(screenState.screenState != ScreenState.Map) {
        screenViewModel.trigger(MapScreenViewModel.Event.BackToMap)
    }

    MapScreenContent(
        screenViewState = screenState,
        onScreenEvent = screenViewModel::trigger
    )
}

@Composable
private fun MapScreenContent(
    screenViewState: MapScreenViewModel.ViewState,
    onScreenEvent: (MapScreenViewModel.Event) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MapContent(Modifier.fillMaxSize())

        MapTopControls(
            modifier = Modifier.align(Alignment.TopEnd),
            onEvent = onScreenEvent
        )

        if (screenViewState.screenState is ScreenState.Message) {
            MessageFloatingCard(
                modifier = Modifier
                    .padding(top = 128.dp)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.9f),
                onMessageSent = { onScreenEvent(MapScreenViewModel.Event.BackToMap) }
            )
        }

        if (screenViewState.showPermissionNotification) {
            Notification(
                notification = stringResource(R.string.permissions_message),
                onAllowClick = { onScreenEvent(MapScreenViewModel.Event.OnPermissionAllow) },
                onDenyClick = { onScreenEvent(MapScreenViewModel.Event.OnPermissionDeny) },
                onDismiss = { onScreenEvent(MapScreenViewModel.Event.OnPermissionDeny) }
            )
        }

        if (screenViewState.showFindMeDialog) {
            FindMeDialog(
                isForcedEnabled = screenViewState.isForcedForegroundEnabled,
                onDismiss = { onScreenEvent(MapScreenViewModel.Event.DismissFindMeDialog) },
                onEnable = { minutes -> onScreenEvent(MapScreenViewModel.Event.EnableForcedForeground(minutes)) },
                onDisable = { onScreenEvent(MapScreenViewModel.Event.DisableForcedForeground) }
            )
        }

        MapBottomControls(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    WhereIsEveryoneTheme {
        MapScreenContent(
            screenViewState = MapScreenViewModel.ViewState(
                showPermissionNotification = true,
                permissions = emptyMap(),
                screenState = ScreenState.Map,
                showFindMeDialog = false,
                isForcedForegroundEnabled = false
            ),
            onScreenEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenMessagePreview() {
    WhereIsEveryoneTheme {
        MapScreenContent(
            screenViewState = MapScreenViewModel.ViewState(
                showPermissionNotification = false,
                permissions = emptyMap(),
                screenState = ScreenState.Message,
                showFindMeDialog = false,
                isForcedForegroundEnabled = false
            ),
            onScreenEvent = {}
        )
    }
}
