package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.common.entities.ScreenState
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainNavGraph
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
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
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
fun MapScreen(
    viewState: MapViewModel.ViewState,
    trigger: (MapViewModel.Command) -> Unit,
) {
    Box {
        Map()
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    WhereIsEveryoneTheme {
        MapScreen(
            MapViewModel.ViewState(
                screenState = ScreenState.Success,
            )
        ) {}
    }
}