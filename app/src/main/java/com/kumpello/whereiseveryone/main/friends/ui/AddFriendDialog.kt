package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.main.friends.presentation.AddFriendViewModel

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
        title = { Text(text = stringResource(R.string.confirmation_title)) },
        text = { Text(text = stringResource(R.string.add_linked_friend_confirmation, username)) },
        modifier = Modifier.padding(32.dp),
        shape = Shapes.small
    )
}
