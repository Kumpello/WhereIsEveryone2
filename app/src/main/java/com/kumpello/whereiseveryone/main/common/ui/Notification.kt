package com.kumpello.whereiseveryone.main.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.AppDialog
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.AppSpacing

@Composable
fun Notification(
    modifier: Modifier = Modifier,
    notification: AnnotatedString,
    onAllowClick: () -> Unit,
    onDenyClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(onDismiss = onDismiss, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
            Text(
                stringResource(R.string.permissions_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(notification, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Button.Secondary(text = stringResource(R.string.deny), modifier = Modifier.weight(1f), onClick = onDenyClick)
                Button.Primary(text = stringResource(R.string.allow), modifier = Modifier.weight(1f), onClick = onAllowClick)
            }
        }
    }
}

@Composable
fun Notification(
    modifier: Modifier = Modifier,
    notification: String,
    onAllowClick: () -> Unit,
    onDenyClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Notification(
        modifier = modifier,
        notification = AnnotatedString(notification),
        onAllowClick = onAllowClick,
        onDenyClick = onDenyClick,
        onDismiss = onDismiss
    )
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
