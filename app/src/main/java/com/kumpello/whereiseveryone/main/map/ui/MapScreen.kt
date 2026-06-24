package com.kumpello.whereiseveryone.main.map.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.MainRoute
import com.kumpello.whereiseveryone.main.common.ui.Notification
import com.kumpello.whereiseveryone.main.map.presentation.MapScreenViewModel
import com.kumpello.whereiseveryone.main.map.presentation.MessageViewModel
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
                is MapScreenViewModel.Action.ShowPermissionSettings -> { /* Handle in Activity */ }
            }
        }
    }

    BackHandler(screenState.screenState != ScreenState.Map) {
        screenViewModel.trigger(MapScreenViewModel.Event.BackToMap)
    }

    MapScreen(
        screenViewState = screenState,
        onScreenEvent = screenViewModel::trigger
    )
}

@Composable
private fun MapScreen(
    screenViewState: MapScreenViewModel.ViewState,
    onScreenEvent: (MapScreenViewModel.Event) -> Unit,
    topControls: @Composable (Modifier) -> Unit = { MapTopControls(modifier = it) },
    bottomControls: @Composable (Modifier) -> Unit = { MapBottomControls(modifier = it) },
    messageFloatingCard: @Composable (Modifier) -> Unit = { 
        MessageFloatingCard(
            modifier = it,
            onMessageSent = { onScreenEvent(MapScreenViewModel.Event.BackToMap) }
        ) 
    },
    mapContent: @Composable (Modifier) -> Unit = {
        MapContent(
            modifier = it
        )
    }
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (screenViewState.showPermissionNotification) {
            Notification(
                modifier = Modifier
                    .zIndex(Float.MAX_VALUE)
                    .align(Alignment.Center)
                    .padding(8.dp),
                notification = stringResource(R.string.permissions_message),
                onAllowClick = { onScreenEvent(MapScreenViewModel.Event.OnPermissionAllow) },
                onDenyClick = { onScreenEvent(MapScreenViewModel.Event.OnPermissionDeny) }
            )
        }

        topControls(Modifier.align(Alignment.TopEnd))

        mapContent(Modifier.align(Alignment.Center))

        if (screenViewState.screenState is ScreenState.Message) {
            messageFloatingCard(
                Modifier
                    .padding(top = 128.dp)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.9f)
            )
        }

        bottomControls(Modifier.align(Alignment.BottomEnd))
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    WhereIsEveryoneTheme {
        MapScreen(
            screenViewState = MapScreenViewModel.ViewState(
                showPermissionNotification = false,
                permissions = emptyMap(),
                screenState = ScreenState.Map
            ),
            onScreenEvent = {},
            topControls = {
                MapTopControls(onEvent = {})
            },
            bottomControls = {
                MapBottomControls(onEvent = {})
            },
            messageFloatingCard = {
                MessageFloatingCard(
                    viewState = MessageViewModel.ViewState(
                        userMessage = "Status",
                        userMessageField = "Draft"
                    ),
                    onEvent = {}
                )
            },
            mapContent = {
                Box(
                    modifier = it
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Map Placeholder")
                }
            }
        )
    }
}
