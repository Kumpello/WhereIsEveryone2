package com.kumpello.whereiseveryone.main.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainNavGraph
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@MainNavGraph(start = false)
@Destination
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
    viewModel: SettingsViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {

    }

    SettingsScreen(
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
fun SettingsScreen(
    viewState: SettingsViewModel.ViewState,
    trigger: (SettingsViewModel.Command) -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    WhereIsEveryoneTheme {
        //Settings(rememberNavController(), MainActivityViewModel())
    }
}