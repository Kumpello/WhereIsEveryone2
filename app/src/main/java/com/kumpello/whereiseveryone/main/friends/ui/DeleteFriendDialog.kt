package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.layout.fillMaxWidth
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
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel

@Composable
fun DeleteFriendDialog(
    friend: Friend,
    trigger: (FriendsViewModel.Event) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back button.
            // If you want to disable that functionality, simply leave this block empty.
            trigger(FriendsViewModel.Event.CloseDeleteFriendDialog)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // perform the confirm action and
                    // close the dialog
                    trigger(FriendsViewModel.Event.DeleteFriend(friend.username))
                    trigger(FriendsViewModel.Event.CloseDeleteFriendDialog)
                }
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // close the dialog
                    trigger(FriendsViewModel.Event.CloseDeleteFriendDialog)
                }
            ) {
                Text(text = stringResource(R.string.dismiss))
            }
        },
        title = {
            Text(text = stringResource(R.string.confirmation_title))
        },
        text = {
            Text(text = stringResource(R.string.delete_friend_confirmation_format, friend.username))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        shape = Shapes.small
    )
}