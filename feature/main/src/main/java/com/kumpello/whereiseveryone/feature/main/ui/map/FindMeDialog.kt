package com.kumpello.whereiseveryone.feature.main.ui.map

import com.kumpello.whereiseveryone.feature.main.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kumpello.whereiseveryone.core.ui.components.AppDialog
import com.kumpello.whereiseveryone.core.ui.components.Button
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@Composable
fun FindMeDialog(
    modifier: Modifier = Modifier,
    isForcedEnabled: Boolean,
    endTime: Long?,
    onDismiss: () -> Unit,
    onEnable: (Long?) -> Unit,
    onDisable: () -> Unit
) {
    val options = listOf(
        1800L to stringResource(R.string.find_me_duration_30),
        3600L to stringResource(R.string.find_me_duration_60),
        5400L to stringResource(R.string.find_me_duration_90),
        null to stringResource(R.string.find_me_duration_indefinite)
    )
    var selectedDuration by rememberSaveable { mutableStateOf<Long?>(1800L) }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isForcedEnabled) {
        while (isForcedEnabled) {
            currentTime = System.currentTimeMillis()
            delay(1.seconds)
        }
    }

    AppDialog(onDismiss = onDismiss, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Text(
                stringResource(R.string.find_me_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.find_me_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    stringResource(R.string.find_me_warning),
                    modifier = Modifier.padding(AppSpacing.sm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (isForcedEnabled) {
                val timeLeftText = if (endTime != null) {
                    val totalSeconds = (endTime - currentTime).coerceAtLeast(0) / 1000
                    stringResource(R.string.time_left_format, totalSeconds / 60, totalSeconds % 60)
                } else stringResource(R.string.time_left_indefinite)
                Text(timeLeftText, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                Column(Modifier.selectableGroup()) {
                    options.forEach { (duration, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = AppSize.touchTarget)
                                .selectable(selected = duration == selectedDuration, role = Role.RadioButton, onClick = { selectedDuration = duration })
                                .padding(vertical = AppSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            RadioButton(selected = duration == selectedDuration, onClick = null)
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Button.Secondary(text = stringResource(R.string.dismiss), modifier = Modifier.weight(1f), onClick = onDismiss)
                Button.Primary(
                    text = stringResource(if (isForcedEnabled) R.string.disable else R.string.confirm),
                    modifier = Modifier.weight(1f)
                ) { if (isForcedEnabled) onDisable() else onEnable(selectedDuration) }
            }
        }
    }
}
