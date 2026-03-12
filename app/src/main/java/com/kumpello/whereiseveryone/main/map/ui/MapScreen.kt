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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.MainNavigation
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = viewModel()
) {
    val state by viewModel.viewState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                MapViewModel.Action.NavigateFriends -> navController.navigate(MainNavigation.Friends.route)
                MapViewModel.Action.NavigateSettings -> navController.navigate(MainNavigation.Settings.route)
                else -> Unit
            }
        }
    }


    BackHandler(true) { //TODO: Add click on map
        viewModel.onEvent(MapViewModel.Event.BackToMap) //TODO Change to exit app?
    }

    MapScreen(
        viewState = state,
        action = viewModel.action,
        event = viewModel::onEvent
    )
}

@Composable
fun MapScreen(
    viewState: MapViewModel.ViewState,
    action: SharedFlow<MapViewModel.Action>,
    event: (MapViewModel.Event) -> Unit,
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
                onClick = { event(MapViewModel.Event.NavigateMessage) },
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
                onClick = { event(MapViewModel.Event.NavigateFriends) },
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
                onClick = { event(MapViewModel.Event.NavigateSettings) },
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
            actions = action,
            userLocation = viewState.user
        )
        when (viewState.screenState) {
            is ScreenState.Message -> MessageFloatingCard(
                viewState = viewState,
                actions = action,
                trigger = event
            )

            else -> {
            //TODO
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(4.dp)
        ) {
            FloatingActionButton(
                onClick = { event(MapViewModel.Event.CenterMap) },
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