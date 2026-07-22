package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

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
    var selectedOption by remember { mutableStateOf(options[0]) }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isForcedEnabled) {
        while (isForcedEnabled) {
            currentTime = System.currentTimeMillis()
            delay(1.seconds)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier.padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.find_me_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.find_me_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (isForcedEnabled) {
                    val timeLeftText = if (endTime != null) {
                        val remainingMillis = (endTime - currentTime).coerceAtLeast(0)
                        val totalSeconds = remainingMillis / 1000
                        val minutes = totalSeconds / 60
                        val seconds = totalSeconds % 60
                        stringResource(R.string.time_left_format, minutes, seconds)
                    } else {
                        stringResource(R.string.time_left_indefinite)
                    }

                    Text(
                        text = timeLeftText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Column(Modifier.selectableGroup()) {
                        options.forEach { option ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .selectable(
                                        selected = (option == selectedOption),
                                        onClick = { selectedOption = option },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == selectedOption),
                                onClick = null
                            )
                            Text(
                                text = option.second,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp),
                                color = if (option.first == null) Color.Red else Color.Unspecified,
                                fontWeight = if (option.first == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button.Animated(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.dismiss)
                    ) {
                        onDismiss()
                    }
                    Button.Animated(
                        modifier = Modifier.weight(1f),
                        text = stringResource(if (isForcedEnabled) R.string.disable else R.string.confirm)
                    ) {
                        if (isForcedEnabled) onDisable() else onEnable(selectedOption.first)
                    }
                }
            }
        }
    }
}
