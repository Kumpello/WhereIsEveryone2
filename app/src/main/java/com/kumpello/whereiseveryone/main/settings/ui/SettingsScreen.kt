package com.kumpello.whereiseveryone.main.settings.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.BuildConfig
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.AuthenticationActivity
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import kotlin.math.ln
import kotlin.math.pow

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

                SettingsViewModel.Action.NavigateToAuth -> {
                    context.startActivity(Intent(context, AuthenticationActivity::class.java))
                    (context as? ComponentActivity)?.finish()
                }
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
    val minDistance = 10f
    val maxDistance = 2000f
    val sliderValue = ln(viewState.proximityDistance.toFloat().coerceAtLeast(minDistance) / minDistance) / ln(maxDistance / minDistance)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Start
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.proximity_distance_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.distance_m_format, viewState.proximityDistance),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { normalizedValue ->
                            val distance = minDistance * (maxDistance / minDistance).pow(normalizedValue)
                            trigger(SettingsViewModel.Event.ChangeProximityDistance(distance.toInt().coerceIn(10, 2000)))
                        },
                        valueRange = 0f..1f
                    )
                }

                Button.Animated(
                    text = stringResource(viewState.locationSwitchTextId),
                    textSize = 18
                ) {
                    trigger(SettingsViewModel.Event.SwitchLocationServiceState)
                }
                Button.Animated(
                    text = stringResource(viewState.deleteLocationDataId),
                    textSize = 18
                ) {
                    trigger(SettingsViewModel.Event.ClearData)
                }
                Button.Animated(
                    text = stringResource(viewState.logoutTextId),
                    textSize = 18
                ) {
                    trigger(SettingsViewModel.Event.Logout)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AboutSection()
        }
    }
}

@Composable
private fun AboutSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(bottom = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.about_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            )
        }

        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )

        val context = LocalContext.current
        Button.Animated(
            text = stringResource(R.string.settings_licenses),
            textSize = 14
        ) {
            context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
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
                deleteLocationDataId = R.string.settings_delete_location_data,
                logoutTextId = R.string.settings_logout,
                proximityDistance = 50
            )
        ) {}
    }
}