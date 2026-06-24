package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.common.ui.TextField
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.main.friends.presentation.AddFriendViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddFriendContent(
    onFriendAdded: () -> Unit,
    viewModel: AddFriendViewModel = koinViewModel()
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            if (action is AddFriendViewModel.Action.NotifyFriendAdded) {
                onFriendAdded()
            }
        }
    }

    AddFriendContent(
        viewState = viewState,
        onEvent = viewModel::trigger
    )
}

@Composable
fun AddFriendContent(
    viewState: AddFriendViewModel.ViewState,
    onEvent: (AddFriendViewModel.Event) -> Unit
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
        Button.Animated(
            text = stringResource(R.string.add_friend),
            width = 150,
            enabled = !viewState.actionState.isLoading
        ) {
            onEvent(AddFriendViewModel.Event.AddFriend)
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
        onEvent = {}
    )
}
