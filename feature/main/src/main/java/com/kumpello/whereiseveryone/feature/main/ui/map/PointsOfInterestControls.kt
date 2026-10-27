package com.kumpello.whereiseveryone.feature.main.ui.map

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.components.AppDialog
import com.kumpello.whereiseveryone.core.ui.components.Button
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.data.model.MAX_POINT_DESCRIPTION_LENGTH
import com.kumpello.whereiseveryone.data.model.MAX_POINT_NAME_LENGTH
import com.kumpello.whereiseveryone.data.model.MAX_POINTS_OF_INTEREST
import com.kumpello.whereiseveryone.data.model.PointOfInterest
import com.kumpello.whereiseveryone.feature.main.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun PointLayerButton(modifier: Modifier = Modifier, ownCount: Int, onClick: () -> Unit) {
    ControlBar(modifier = modifier) {
        ControlButton(
            imageVector = Icons.Default.Flag,
            contentDescription = stringResource(R.string.poi_layer_button, ownCount, MAX_POINTS_OF_INTEREST),
            badgeCount = ownCount,
            onClick = onClick,
        )
    }
}

@Composable
internal fun PointManagerDialog(state: PointsOfInterestState, onEvent: (MapViewModel.Event) -> Unit) {
    AppDialog(onDismiss = { onEvent(MapViewModel.Event.DismissPointManager) }) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(stringResource(R.string.poi_title), style = MaterialTheme.typography.headlineSmall)
            Row(modifier = Modifier.fillMaxWidth().toggleable(
                value = state.showLayer, role = Role.Switch,
                onValueChange = { onEvent(MapViewModel.Event.SetPointLayerVisible(it)) },
            )) {
                Text(stringResource(R.string.poi_show_layer), modifier = Modifier.weight(1f))
                Switch(checked = state.showLayer, onCheckedChange = null)
            }
            Text(stringResource(R.string.poi_add_hint), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.poi_own_count, state.ownPoints.size, MAX_POINTS_OF_INTEREST), style = MaterialTheme.typography.titleSmall)
            if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            state.ownPoints.forEach { point ->
                ListItem(
                    modifier = Modifier.clickable(role = Role.Button) { onEvent(MapViewModel.Event.OnPointClick(point.id)) },
                    headlineContent = { Text(point.name) },
                    leadingContent = { Icon(Icons.Default.Flag, contentDescription = null) },
                )
            }
            if (state.ownPoints.isEmpty() && state.isLoaded) Text(stringResource(R.string.poi_no_points))
            Button.Secondary(text = stringResource(R.string.poi_refresh), enabled = !state.isLoading) {
                onEvent(MapViewModel.Event.LoadOwnPoints)
            }
            Button.Secondary(text = stringResource(R.string.close)) { onEvent(MapViewModel.Event.DismissPointManager) }
        }
    }
}

@Composable
internal fun PointEditorDialog(state: PointsOfInterestState, onEvent: (MapViewModel.Event) -> Unit) {
    val draft = state.draft ?: return
    val nameLength = draft.name.codePointCount(0, draft.name.length)
    val descriptionLength = draft.description.codePointCount(0, draft.description.length)
    AppDialog(onDismiss = { onEvent(MapViewModel.Event.DismissPointEditor) }) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(stringResource(if (draft.editingId == null) R.string.poi_add_title else R.string.poi_edit_title),
                style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(), value = draft.name,
                onValueChange = { onEvent(MapViewModel.Event.ChangePointName(it)) },
                label = { Text(stringResource(R.string.poi_name)) }, singleLine = true, enabled = !state.isSaving,
                isError = nameLength > MAX_POINT_NAME_LENGTH,
                supportingText = { Text(stringResource(R.string.poi_text_count, nameLength, MAX_POINT_NAME_LENGTH)) },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(), value = draft.description,
                onValueChange = { onEvent(MapViewModel.Event.ChangePointDescription(it)) },
                label = { Text(stringResource(R.string.poi_description)) }, minLines = 3, maxLines = 6, enabled = !state.isSaving,
                isError = descriptionLength > MAX_POINT_DESCRIPTION_LENGTH,
                supportingText = { Text(stringResource(R.string.poi_text_count, descriptionLength, MAX_POINT_DESCRIPTION_LENGTH)) },
            )
            Text(stringResource(R.string.poi_sharing_hint), style = MaterialTheme.typography.bodySmall)
            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Button.Primary(text = stringResource(R.string.poi_save), enabled = draft.isValid() && !state.isLoading, loading = state.isSaving) {
                onEvent(MapViewModel.Event.SavePoint)
            }
            Button.Secondary(text = stringResource(R.string.close), enabled = !state.isSaving) {
                onEvent(MapViewModel.Event.DismissPointEditor)
            }
        }
    }
}

@Composable
internal fun PointDetailsDialog(point: PointOfInterest, onEvent: (MapViewModel.Event) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val createdAt = remember(point.createdAt, locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
            .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(point.createdAt))
    }
    AppDialog(onDismiss = { onEvent(MapViewModel.Event.DismissPointDetails) }) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(point.name, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.poi_creator, point.creatorUsername), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.poi_created, createdAt), style = MaterialTheme.typography.bodyMedium)
            Text(point.description.ifBlank { stringResource(R.string.poi_no_description) }, style = MaterialTheme.typography.bodyLarge)
            Button.Primary(text = stringResource(R.string.navigate_action)) { onEvent(MapViewModel.Event.NavigateToPoint(point.id)) }
            if (point.isOwn) {
                Button.Secondary(text = stringResource(R.string.poi_edit)) { onEvent(MapViewModel.Event.EditSelectedPoint) }
                Button.Secondary(text = stringResource(R.string.poi_delete), destructive = true) { onEvent(MapViewModel.Event.RequestPointDeletion) }
            }
            Button.Secondary(text = stringResource(R.string.close)) { onEvent(MapViewModel.Event.DismissPointDetails) }
        }
    }
}

@Composable
internal fun DeletePointDialog(point: PointOfInterest, state: PointsOfInterestState, onEvent: (MapViewModel.Event) -> Unit) {
    AppDialog(onDismiss = { onEvent(MapViewModel.Event.CancelPointDeletion) }) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(stringResource(R.string.poi_delete), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.poi_delete_confirmation, point.name))
            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Button.Primary(text = stringResource(R.string.poi_delete), enabled = !state.isLoading, loading = state.isSaving) {
                onEvent(MapViewModel.Event.DeletePoint)
            }
            Button.Secondary(text = stringResource(R.string.close), enabled = !state.isSaving) { onEvent(MapViewModel.Event.CancelPointDeletion) }
        }
    }
}

private val previewPoint = PointOfInterest("sample", "Riverside meeting place", "Meet beside the bridge.", 52.23, 21.01, "anna", 1_800_000_000_000L, true)

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PointManagerPreview() {
    WhereIsEveryoneTheme { PointManagerDialog(PointsOfInterestState(ownPoints = listOf(previewPoint), isLoaded = true), {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PointEditorPreview() {
    WhereIsEveryoneTheme { PointEditorDialog(PointsOfInterestState(draft = PointDraft(52.23, 21.01, "Riverside meeting place", "Meet beside the bridge.")), {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PointDetailsPreview() {
    WhereIsEveryoneTheme { PointDetailsDialog(previewPoint, {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DeletePointPreview() {
    WhereIsEveryoneTheme { DeletePointDialog(previewPoint, PointsOfInterestState(), {}) }
}

@Preview(showBackground = true)
@Composable
private fun PointLayerButtonPreview() {
    WhereIsEveryoneTheme { PointLayerButton(ownCount = 2, onClick = {}) }
}
