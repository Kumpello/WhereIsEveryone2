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
import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel

@Composable
fun DeleteFriendDialog(
    friend: Friend,
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back button.
            // If you want to disable that functionality, simply leave this block empty.
            trigger(FriendsViewModel.Command.CloseDeleteFriendDialog)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // perform the confirm action and
                    // close the dialog
                    trigger(FriendsViewModel.Command.DeleteFriend(friend.nick))
                    trigger(FriendsViewModel.Command.CloseDeleteFriendDialog)
                }
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // close the dialog
                    trigger(FriendsViewModel.Command.CloseDeleteFriendDialog)
                }
            ) {
                Text(text = stringResource(R.string.dismiss))
            }
        },
        title = {
            Text(text = "Confirmation")
        },
        text = {
            Text(text = "Are you sure you want to delete $friend.nick from your friends?")
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        shape = Shapes.small
    )
}