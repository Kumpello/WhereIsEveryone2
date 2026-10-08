package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel

@Composable
fun DeleteFriendDialog(friend: Friend, trigger: (FriendsViewModel.Event) -> Unit) {
    AlertDialog(
        onDismissRequest = { trigger(FriendsViewModel.Event.CloseDeleteFriendDialog) },
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                onClick = {
                    trigger(FriendsViewModel.Event.DeleteFriend(friend.username))
                    trigger(FriendsViewModel.Event.CloseDeleteFriendDialog)
                }
            ) { Text(stringResource(R.string.delete_cd)) }
        },
        dismissButton = {
            TextButton(onClick = { trigger(FriendsViewModel.Event.CloseDeleteFriendDialog) }) {
                Text(stringResource(R.string.dismiss))
            }
        },
        title = { Text(stringResource(R.string.remove_friend_title)) },
        text = { Text(stringResource(R.string.delete_friend_confirmation_format, friend.username)) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    )
}
