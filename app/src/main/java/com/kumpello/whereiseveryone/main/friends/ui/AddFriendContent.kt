package com.kumpello.whereiseveryone.main.friends.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.common.ui.TextField
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.main.friends.presentation.AddFriendViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.core.net.toUri

@Composable
fun AddFriendContent(
    onFriendAdded: () -> Unit,
    onOpenNfcReading: () -> Unit,
    viewModel: AddFriendViewModel = koinViewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scanner = remember {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                is AddFriendViewModel.Action.NotifyFriendAdded -> onFriendAdded()
                is AddFriendViewModel.Action.Toast -> {
                    Toast.makeText(context, action.id, Toast.LENGTH_SHORT).show()
                }

                AddFriendViewModel.Action.OpenQrScanner -> {
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            barcode.rawValue?.let { rawValue ->
                                val uri = rawValue.toUri()
                                viewModel.trigger(AddFriendViewModel.Event.OnUriReceived(uri))
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Scanning failed", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    AddFriendContent(
        viewState = viewState,
        onEvent = viewModel::trigger,
        onOpenNfcReading = onOpenNfcReading
    )
}

@Composable
fun AddFriendContent(
    viewState: AddFriendViewModel.ViewState,
    onEvent: (AddFriendViewModel.Event) -> Unit,
    onOpenNfcReading: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextField.Regular(
            label = stringResource(R.string.your_friends_nick),
            value = viewState.addFriendNick,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            ),
            onValueChange = { nick ->
                onEvent(AddFriendViewModel.Event.SetAddFriendNick(nick))
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button.Animated(
                text = stringResource(R.string.add_friend),
                width = 150,
                enabled = !viewState.actionState.isLoading
            ) {
                onEvent(AddFriendViewModel.Event.AddFriend)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onEvent(AddFriendViewModel.Event.ScanQrCode) },
                enabled = !viewState.actionState.isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR Code",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onOpenNfcReading,
                enabled = !viewState.actionState.isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = "Read NFC",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddFriendContentPreview() {
    AddFriendContent(
        viewState = AddFriendViewModel.ViewState(
            addFriendNick = "Papator2000",
            actionState = AsyncState.Idle
        ),
        onEvent = {},
        onOpenNfcReading = {}
    )
}
