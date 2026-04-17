package com.kumpello.whereiseveryone.main.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                SettingsViewModel.Action.BackToMap -> navController.popBackStack()
            }
        }
    }

    SettingsScreen(
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
private fun SettingsScreen(
    viewState: SettingsViewModel.ViewState,
    trigger: (SettingsViewModel.Command) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button.Animated(
                    text = viewState.locationSwitchText,
                ) {
                    trigger(SettingsViewModel.Command.SwitchLocationServiceState)
                }
                Button.Animated(
                    text = viewState.deleteLocationData,
                ) {
                    trigger(SettingsViewModel.Command.ClearData)
                }
            }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    WhereIsEveryoneTheme(darkTheme = true) {
        SettingsScreen(
            viewState = SettingsViewModel.ViewState(
                isLocationServiceRunning = true,
                locationSwitchText = "Stop sharing location",
                deleteLocationData = "Delete your location data"
            )
        ) {}
    }
}