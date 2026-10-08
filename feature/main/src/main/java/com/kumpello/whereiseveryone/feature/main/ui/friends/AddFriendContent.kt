package com.kumpello.whereiseveryone.feature.main.ui.friends

import com.kumpello.whereiseveryone.feature.main.R
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.kumpello.whereiseveryone.core.ui.components.TextField
import com.kumpello.whereiseveryone.core.presentation.AsyncState
import com.kumpello.whereiseveryone.core.ui.components.Button
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import org.koin.compose.viewmodel.koinViewModel

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
                            Toast.makeText(
                                context,
                                context.applicationContext.getString(R.string.scanning_failed),
                                Toast.LENGTH_SHORT
                            ).show()
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
    val focusManager = LocalFocusManager.current
    val enabled = !viewState.actionState.isLoading
    Column(
        modifier = Modifier.fillMaxWidth().padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        TextField.Regular(
            label = stringResource(R.string.your_friends_nick),
            value = viewState.addFriendNick,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (enabled) onEvent(AddFriendViewModel.Event.AddFriend)
            }),
            onValueChange = { onEvent(AddFriendViewModel.Event.SetAddFriendNick(it)) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Button.Primary(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.add_friend),
                enabled = enabled,
                loading = viewState.actionState.isLoading
            ) {
                focusManager.clearFocus()
                onEvent(AddFriendViewModel.Event.AddFriend)
            }
            FilledTonalIconButton(
                modifier = Modifier.size(AppSize.button),
                onClick = { onEvent(AddFriendViewModel.Event.ScanQrCode) },
                enabled = enabled
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scan_qr_code_cd), modifier = Modifier.size(AppSize.icon))
            }
            FilledTonalIconButton(modifier = Modifier.size(AppSize.button), onClick = onOpenNfcReading, enabled = enabled) {
                Icon(Icons.Default.Contactless, contentDescription = stringResource(R.string.read_nfc_cd), modifier = Modifier.size(AppSize.icon))
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
