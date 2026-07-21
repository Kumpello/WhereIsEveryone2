package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.Shapes

@Composable
fun QrCodeDialog(
    username: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
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
                    text = stringResource(R.string.your_qr_code),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                val qrContent = "whereiseveryone://addfriend/$username"
                val qrBitmap = remember(qrContent) {
                    QrCodeGenerator.generateQrCode(qrContent, 512)
                }

                qrBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = stringResource(R.string.qr_code_cd),
                        modifier = Modifier.size(200.dp)
                    )
                } ?: Text(stringResource(R.string.error_generating_qr))

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.scan_this_to_add_me_as_a_friend),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button.Animated(text = stringResource(R.string.close), width = 150) {
                    onDismiss()
                }
            }
        }
    }
}
