package com.kumpello.whereiseveryone.feature.main.ui.friends

import com.kumpello.whereiseveryone.feature.main.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme

@Composable
fun NfcReadingDialog(onDismiss: () -> Unit) {
    NfcDialog(
        title = stringResource(R.string.reading_nfc_tag_title),
        message = stringResource(R.string.reading_nfc_tag_message),
        onDismiss = onDismiss
    )
}

@Preview(showBackground = true)
@Composable
fun NfcReadingDialogPreview() {
    WhereIsEveryoneTheme { NfcReadingDialog(onDismiss = {}) }
}
