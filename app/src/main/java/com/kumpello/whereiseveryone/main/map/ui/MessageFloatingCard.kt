package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.common.ui.TextField
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.map.presentation.MessageViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MessageFloatingCard(
    modifier: Modifier = Modifier,
    onMessageSent: () -> Unit
) {
    if (LocalInspectionMode.current) {
        MessageFloatingCard(
            modifier = modifier,
            viewState = MessageViewModel.ViewState(
                userMessage = stringResource(R.string.status_label),
                userMessageField = stringResource(R.string.draft_label)
            ),
            onEvent = {}
        )
        return
    }

    val viewModel: MessageViewModel = koinViewModel()
    val viewState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            if (action is MessageViewModel.Action.NotifyMessageSent) {
                onMessageSent()
            }
        }
    }

    MessageFloatingCard(
        modifier = modifier,
        viewState = viewState,
        onEvent = viewModel::trigger
    )
}

@Composable
fun MessageFloatingCard(
    modifier: Modifier = Modifier,
    viewState: MessageViewModel.ViewState,
    onEvent: (MessageViewModel.Event) -> Unit,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.your_message),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = viewState.userMessage,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.size(8.dp))
            TextField.Regular(
                label = "",
                value = viewState.userMessageField,
                onValueChange = { onEvent(MessageViewModel.Event.WriteMessage(it)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
            Spacer(Modifier.size(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button.Animated(
                    text = stringResource(R.string.clear_message),
                    width = 120,
                    onClick = { onEvent(MessageViewModel.Event.ClearMessage) }
                )
                Button.Animated(
                    text = stringResource(R.string.update_message),
                    width = 120,
                    onClick = { onEvent(MessageViewModel.Event.SendMessage) }
                )
            }
        }
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
