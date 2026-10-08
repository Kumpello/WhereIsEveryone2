package com.kumpello.whereiseveryone.main.friends.ui

import android.nfc.NfcAdapter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.main.friends.presentation.ShareProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShareProfileContent(
    onShowQr: () -> Unit,
    onTriggerNfc: () -> Unit,
    viewModel: ShareProfileViewModel = koinViewModel()
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    ShareProfileContent(
        viewState = viewState,
        onEvent = viewModel::trigger,
        onShowQr = onShowQr,
        onTriggerNfc = onTriggerNfc
    )
}

@Composable
fun ShareProfileContent(
    viewState: ShareProfileViewModel.ViewState,
    onEvent: (ShareProfileViewModel.Event) -> Unit,
    onShowQr: () -> Unit,
    onTriggerNfc: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Text(
            text = stringResource(R.string.your_username_format, viewState.username),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button.Secondary(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.show_my_qr_cd),
                icon = Icons.Default.QrCode,
                onClick = onShowQr
            )
            Button.Secondary(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.nfc_share_cd),
                icon = Icons.Default.Contactless
            ) {
                val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                when {
                    nfcAdapter == null -> onEvent(ShareProfileViewModel.Event.OnNfcNotSupported)
                    !nfcAdapter.isEnabled -> onEvent(ShareProfileViewModel.Event.OnNfcDisabled)
                    else -> onTriggerNfc()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShareProfileContentPreview() {
    ShareProfileContent(
        viewState = ShareProfileViewModel.ViewState(
            username = "Janusz"
        ),
        onEvent = {},
        onShowQr = {},
        onTriggerNfc = {}
    )
}
