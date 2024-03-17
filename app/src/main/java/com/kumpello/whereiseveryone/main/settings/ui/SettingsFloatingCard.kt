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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.map.ui.FloatingCard
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun SettingsFloatingCard(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {

    }

    SettingsFloatingCard(
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
private fun SettingsFloatingCard(
    modifier: Modifier = Modifier,
    viewState: SettingsViewModel.ViewState,
    trigger: (SettingsViewModel.Command) -> Unit,
) {
    FloatingCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    WhereIsEveryoneTheme {
        SettingsFloatingCard(
            viewState = SettingsViewModel.ViewState(
                mock = "Dupa"
            )
        ) {}
    }
}