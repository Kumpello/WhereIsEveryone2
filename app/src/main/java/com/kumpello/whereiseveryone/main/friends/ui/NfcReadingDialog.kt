package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme

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
