package com.kumpello.whereiseveryone.main.map.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.common.ui.TextField
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.map.presentation.MessageViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MessageFloatingCard(
    modifier: Modifier = Modifier,
    onMessageSent: () -> Unit,
    onClose: () -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        MessageFloatingCard(
            modifier = modifier,
            viewState = MessageViewModel.ViewState(
                userMessage = stringResource(R.string.status_label),
                userMessageField = stringResource(R.string.draft_label)
            ),
            onEvent = {},
            onClose = onClose
        )
        return
    }

    val context = LocalContext.current
    val viewModel: MessageViewModel = koinViewModel()
    val viewState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                MessageViewModel.Action.NotifyMessageSent -> onMessageSent()
                is MessageViewModel.Action.Toast -> Toast.makeText(context, action.id, Toast.LENGTH_SHORT).show()
            }
        }
    }

    MessageFloatingCard(
        modifier = modifier,
        viewState = viewState,
        onEvent = viewModel::trigger,
        onClose = onClose
    )
}

@Composable
fun MessageFloatingCard(
    modifier: Modifier = Modifier,
    viewState: MessageViewModel.ViewState,
    onEvent: (MessageViewModel.Event) -> Unit,
    onClose: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Text(
            stringResource(R.string.your_message),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            viewState.userMessage.ifBlank { stringResource(R.string.no_status_message) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextField.Regular(
            label = stringResource(R.string.status_label),
            value = viewState.userMessageField,
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            onValueChange = { onEvent(MessageViewModel.Event.WriteMessage(it)) }
        )
        Button.Primary(text = stringResource(R.string.update_message)) {
            focusManager.clearFocus()
            onEvent(MessageViewModel.Event.SendMessage)
        }
        Button.Secondary(text = stringResource(R.string.clear_message)) {
            focusManager.clearFocus()
            onEvent(MessageViewModel.Event.ClearMessage)
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close)) }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageFloatingCardPreview() {
    WhereIsEveryoneTheme {
        MessageFloatingCard(
            viewState = MessageViewModel.ViewState(
                userMessage = stringResource(R.string.status_label),
                userMessageField = stringResource(R.string.draft_label)
            ),
            onEvent = {}
        )
    }
}
