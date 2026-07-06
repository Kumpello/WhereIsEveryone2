package com.kumpello.whereiseveryone.main.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button

@Composable
fun Notification(
    modifier: Modifier = Modifier,
    notification: String,
    onAllowClick: () -> Unit,
    onDenyClick: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                    text = notification,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button.Animated(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.deny)
                    ) {
                        onDenyClick()
                    }
                    Button.Animated(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.allow)
                    ) {
                        onAllowClick()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationPreview() {
    Notification(
        modifier = Modifier
            .zIndex(Float.MAX_VALUE)
            .padding(8.dp),
        notification = stringResource(R.string.permissions_message),
        onAllowClick = { },
        onDenyClick = { },
        onDismiss = { }
    )
}