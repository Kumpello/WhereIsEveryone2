package com.kumpello.whereiseveryone.main.map.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainNavGraph
import com.kumpello.whereiseveryone.main.friends.ui.FriendsFloatingCard
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.settings.ui.SettingsFloatingCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow

@MainNavGraph(start = true)
@Destination
@Composable
fun MapScreen(
    navigator: DestinationsNavigator,
    viewModel: MapViewModel
) {
    val state by viewModel.viewState.collectAsState()

    LaunchedEffect(viewModel.action) {
        viewModel.action.collect { action ->
            when (action) {
                MapViewModel.Action.CenterMap -> {
                    //action in Map
                }
            }
        }
    }


    BackHandler(true) { //TODO: Add click on map
        viewModel.trigger(MapViewModel.Command.BackToMap)
    }

    MapScreen(
        navigator = navigator,
        viewState = state,
        actions = viewModel.action,
        trigger = viewModel::trigger
    )
}

@Composable
fun MapScreen(
    navigator: DestinationsNavigator,
    viewState: MapViewModel.ViewState,
    actions: SharedFlow<MapViewModel.Action>,
    trigger: (MapViewModel.Command) -> Unit,
) {
    Box {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(
                    end = 8.dp
                )
                .zIndex(1000f)
        ) {
            FloatingActionButton(
                onClick = { trigger(MapViewModel.Command.NavigateMessage) },
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Message"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { trigger(MapViewModel.Command.NavigateFriends) },
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    imageVector = Icons.Default.Person,
                    contentDescription = "Friends"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { trigger(MapViewModel.Command.NavigateSettings) },
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
        Map(
            modifier = Modifier.align(Alignment.Center),
            state = viewState.mapSettings,
            actions = actions,
            userLocation = viewState.user
        )
        when (viewState.screenState) {
            ScreenState.Friends -> FriendsFloatingCard()
            is ScreenState.Settings -> SettingsFloatingCard(
                navigator = navigator,
                viewModel = viewState.screenState.settingsViewModel
            )
            is ScreenState.Message -> MessageFloatingCard(
                viewState = viewState,
                actions = actions,
                trigger = trigger
            )

            else -> {}
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(4.dp)
        ) {
            FloatingActionButton(
                onClick = { trigger(MapViewModel.Command.CenterMap) },
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Center"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() { //TODO Fix map preview, or maybe mock map somehow?
    WhereIsEveryoneTheme {
/*        MapScreen(
            navigator = rememberNavController(),
            MapViewModel.ViewState(
                screenState = ScreenState.Map,
            )
        ) {}*/
    }
}