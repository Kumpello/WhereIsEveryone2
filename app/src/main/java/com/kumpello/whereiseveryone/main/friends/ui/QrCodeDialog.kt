package com.kumpello.whereiseveryone.main.friends.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.extension.createAddFriendDeepLink
import com.kumpello.whereiseveryone.common.ui.entity.AppDialog
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.AppSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class QrImage(val loading: Boolean = true, val bitmap: Bitmap? = null)

@Composable
fun QrCodeDialog(username: String, onDismiss: () -> Unit) {
    val qrContent = createAddFriendDeepLink(username)
    val qr by produceState(initialValue = QrImage(), key1 = qrContent) {
        value = QrImage()
        value = QrImage(loading = false, bitmap = withContext(Dispatchers.Default) {
            QrCodeGenerator.generateQrCode(qrContent, 512)
        })
    }
    AppDialog(onDismiss = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.your_qr_code),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(username, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Box(modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                val bitmap = qr.bitmap
                when {
                    qr.loading -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    bitmap != null -> Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.qr_code_cd),
                        modifier = Modifier.fillMaxWidth()
                    )
                    else -> Text(stringResource(R.string.error_generating_qr), color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                stringResource(R.string.scan_this_to_add_me_as_a_friend),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button.Secondary(text = stringResource(R.string.close), onClick = onDismiss)
        }
    }
}
