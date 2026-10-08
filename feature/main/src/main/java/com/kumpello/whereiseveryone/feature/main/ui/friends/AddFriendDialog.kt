package com.kumpello.whereiseveryone.feature.main.ui.friends

import com.kumpello.whereiseveryone.feature.main.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun AddFriendDialog(
    username: String,
    trigger: (AddFriendViewModel.Event) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { trigger(AddFriendViewModel.Event.DismissLinkedFriend) },
        confirmButton = {
            TextButton(onClick = { trigger(AddFriendViewModel.Event.ConfirmLinkedFriend(username)) }) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { trigger(AddFriendViewModel.Event.DismissLinkedFriend) }) {
                Text(text = stringResource(R.string.dismiss))
            }
        },
        title = { Text(text = stringResource(R.string.add_friend)) },
        text = { Text(text = stringResource(R.string.add_linked_friend_confirmation, username)) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    )
}
