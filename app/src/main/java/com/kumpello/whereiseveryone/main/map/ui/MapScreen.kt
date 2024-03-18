package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainNavGraph
import com.kumpello.whereiseveryone.main.friends.ui.FriendsFloatingCard
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.settings.ui.SettingsFloatingCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@MainNavGraph(start = true)
@Destination
@Composable
fun MapScreen(
    navigator: DestinationsNavigator,
    viewModel: MapViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {

    }

    MapScreen(
        navigator =navigator,
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
fun MapScreen(
    navigator: DestinationsNavigator,
    viewState: MapViewModel.ViewState,
    trigger: (MapViewModel.Command) -> Unit,
) {
    Box {
        Row(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            FloatingActionButton(
                onClick = { trigger(MapViewModel.Command.NavigateFriends) },
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = "Friends")
            }
            FloatingActionButton(
                onClick = { trigger(MapViewModel.Command.NavigateSettings) },
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        Map(
            modifier = Modifier.align(Alignment.Center)
        )
        when(viewState.screenState) { //TODO: Clear with back action!!!
            ScreenState.Friends -> FriendsFloatingCard()
            ScreenState.Settings -> SettingsFloatingCard(
                navigator = navigator
            )
            else -> {}
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {

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