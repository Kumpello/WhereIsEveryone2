package com.kumpello.whereiseveryone.main.friends.ui

import android.nfc.NfcAdapter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Your username: ${viewState.username}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onShowQr
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Show My QR",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }
            IconButton(
                onClick = {
                    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                    when {
                        nfcAdapter == null -> onEvent(ShareProfileViewModel.Event.OnNfcNotSupported)
                        !nfcAdapter.isEnabled -> onEvent(ShareProfileViewModel.Event.OnNfcDisabled)
                        else -> onTriggerNfc()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = "NFC Share",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
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
