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
import androidx.compose.runtime.getValue
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

@Composable
fun FindMeDialog(
    modifier: Modifier = Modifier,
    isForcedEnabled: Boolean,
    onDismiss: () -> Unit,
    onEnable: (Int?) -> Unit,
    onDisable: () -> Unit
) {
    val options = listOf(
        30 to stringResource(R.string.find_me_duration_30),
        60 to stringResource(R.string.find_me_duration_60),
        90 to stringResource(R.string.find_me_duration_90),
        null to stringResource(R.string.find_me_duration_indefinite)
    )
    var selectedOption by remember { mutableStateOf(options[0]) }

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
                        text = stringResource(R.string.confirm)
                    ) {
                        onEnable(selectedOption.first)
                    }
                }

                if (isForcedEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button.Animated(
                        modifier = Modifier.fillMaxWidth(),
                        width = 300,
                        text = stringResource(R.string.disable_forced_foreground)
                    ) {
                        onDisable()
                    }
                }
            }
        }
    }
}
