package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme

@Composable
fun NfcSharingDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = Shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.nfc_sharing_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.nfc_sharing_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button.Animated(text = stringResource(R.string.dismiss), width = 150) {
                    onDismiss()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NfcSharingDialogPreview() {
    WhereIsEveryoneTheme(false) {
        NfcSharingDialog(onDismiss = {})
    }
}

@Preview(showBackground = true)
@Composable
fun NfcSharingDialogPreviewDark() {
    WhereIsEveryoneTheme(true) {
        NfcSharingDialog(onDismiss = {})
    }
}
