package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.AppDialog
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme

@Composable
fun NfcSharingDialog(onDismiss: () -> Unit) {
    NfcDialog(
        title = stringResource(R.string.nfc_sharing_title),
        message = stringResource(R.string.nfc_sharing_message_active),
        onDismiss = onDismiss
    )
}

@Composable
internal fun NfcDialog(title: String, message: String, onDismiss: () -> Unit) {
    AppDialog(onDismiss = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Icon(Icons.Default.Contactless, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button.Secondary(text = stringResource(R.string.dismiss), onClick = onDismiss)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NfcSharingDialogPreview() {
    WhereIsEveryoneTheme { NfcSharingDialog(onDismiss = {}) }
}
