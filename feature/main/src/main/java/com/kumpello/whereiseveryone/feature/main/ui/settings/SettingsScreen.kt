package com.kumpello.whereiseveryone.feature.main.ui.settings

import com.kumpello.whereiseveryone.feature.main.R
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.kumpello.whereiseveryone.core.ui.components.Button
import com.kumpello.whereiseveryone.core.ui.components.ScreenHeader
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun SettingsScreen(
    navController: NavController,
    versionName: String,
    onLoggedOut: () -> Unit,
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
                    onLoggedOut()
                }
            }
        }
    }

    SettingsScreen(
        viewState = state,
        versionName = versionName,
        trigger = viewModel::trigger,
        onBack = { navController.popBackStack() }
    )
}

@Composable
private fun SettingsScreen(
    viewState: SettingsViewModel.ViewState,
    versionName: String,
    onBack: () -> Unit = {},
    trigger: (SettingsViewModel.Event) -> Unit
) {
    val minDistance = 10f
    val maxDistance = 2000f
    val sliderValue = ln(viewState.proximityDistance.toFloat().coerceIn(minDistance, maxDistance) / minDistance) / ln(maxDistance / minDistance)
    val proximityLabel = stringResource(R.string.proximity_distance_label)
    val distanceLabel = stringResource(R.string.distance_m_format, viewState.proximityDistance)

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ScreenHeader(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
                modifier = Modifier.widthIn(max = AppSize.contentMaxWidth).padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = AppSpacing.lg, end = AppSpacing.lg, bottom = AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            item {
                SettingsCard {
                    SectionTitle(stringResource(R.string.location_sharing))
                    SettingToggle(
                        title = stringResource(R.string.location_service_label),
                        description = stringResource(R.string.location_service_description),
                        checked = viewState.isLocationServiceRunning,
                        onToggle = { trigger(SettingsViewModel.Event.SwitchLocationServiceState) }
                    )
                    if (viewState.isLocationServiceRunning) {
                        SettingToggle(
                            title = stringResource(R.string.location_sharing),
                            description = stringResource(R.string.sharing_description),
                            checked = viewState.isSharingEnabled,
                            onToggle = { trigger(SettingsViewModel.Event.ToggleSharing) }
                        )
                    }
                }
            }
            item {
                SettingsCard {
                    SectionTitle(proximityLabel)
                    Text(
                        text = distanceLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.proximity_distance_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { value ->
                            val distance = minDistance * (maxDistance / minDistance).pow(value)
                            trigger(SettingsViewModel.Event.ChangeProximityDistance(distance.toInt().coerceIn(10, 2000)))
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.semantics {
                            contentDescription = proximityLabel
                            stateDescription = distanceLabel
                        }
                    )
                }
            }
            item {
                SettingsCard {
                    SectionTitle(stringResource(R.string.account_section_title))
                    Button.Secondary(text = stringResource(viewState.deleteLocationDataId), destructive = true) {
                        trigger(SettingsViewModel.Event.ClearData)
                    }
                    Button.Secondary(text = stringResource(viewState.logoutTextId)) {
                        trigger(SettingsViewModel.Event.Logout)
                    }
                }
            }
            item { SettingsCard { AboutSection(versionName) } }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.widthIn(max = AppSize.contentMaxWidth).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun SettingToggle(title: String, description: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = AppSize.touchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .padding(vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun AboutSection(versionName: String) {
    val context = LocalContext.current
    SectionTitle(stringResource(R.string.about_title))
    Text(
        text = stringResource(R.string.about_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    TextButton(onClick = { context.startActivity(Intent(context, OssLicensesMenuActivity::class.java)) }) {
        Text(stringResource(R.string.settings_licenses))
    }
    Text(
        text = stringResource(R.string.version_format, versionName),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 2f)
@Preview(name = "Landscape", showBackground = true, widthDp = 740, heightDp = 360)
@Composable
fun SettingsPreview() {
    WhereIsEveryoneTheme {
        SettingsScreen(
            versionName = "Preview",
            viewState = SettingsViewModel.ViewState(
                isLocationServiceRunning = true,
                isSharingEnabled = true,
                locationSwitchTextId = R.string.settings_stop_location_service,
                sharingSwitchTextId = R.string.settings_stop_sharing_location,
                deleteLocationDataId = R.string.settings_delete_location_data,
                logoutTextId = R.string.settings_logout,
                proximityDistance = 50
            )
        ) {}
    }
}
