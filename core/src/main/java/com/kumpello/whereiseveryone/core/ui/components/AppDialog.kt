package com.kumpello.whereiseveryone.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kumpello.whereiseveryone.core.ui.theme.AppElevation
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing

@Composable
fun AppDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = modifier.padding(AppSpacing.lg).widthIn(max = AppSize.dialogMaxWidth).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = AppElevation.dialog
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(AppSpacing.lg),
                content = content
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppDialogPreview() {
    WhereIsEveryoneTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppDialog(onDismiss = {}) {
                Text("Dialog title", style = MaterialTheme.typography.headlineSmall)
                Text("A short message explaining the action.", modifier = Modifier.padding(vertical = AppSpacing.md))
                Button.Primary(text = "Confirm", onClick = {})
            }
        }
    }
}
