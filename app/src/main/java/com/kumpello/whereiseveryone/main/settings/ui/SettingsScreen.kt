package com.kumpello.whereiseveryone.main.settings.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                SettingsViewModel.Action.BackToMap -> navController.popBackStack()
                is SettingsViewModel.Action.Toast -> Toast.makeText(
                    context,
                    action.id,
                    Toast.LENGTH_SHORT,
                ).show()
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
    trigger: (SettingsViewModel.Event) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button.Animated(
                text = stringResource(viewState.locationSwitchTextId),
                textSize = 20
            ) {
                trigger(SettingsViewModel.Event.SwitchLocationServiceState)
            }
            Button.Animated(
                text = stringResource(viewState.deleteLocationDataId),
                textSize = 20
            ) {
                trigger(SettingsViewModel.Event.ClearData)
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
                locationSwitchTextId = R.string.settings_stop_sharing_location,
                deleteLocationDataId = R.string.settings_delete_location_data
            )
        ) {}
    }
}