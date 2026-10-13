package com.kumpello.whereiseveryone.feature.main.ui.friends

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.data.model.FriendState
import com.kumpello.whereiseveryone.feature.main.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kumpello.whereiseveryone.feature.main.ui.model.Friend

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

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 480, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DeleteFriendDialogPreview() {
    WhereIsEveryoneTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DeleteFriendDialog(
                friend = Friend(
                    username = "Anna",
                    status = "Available",
                    state = FriendState.ACCEPTED,
                    location = null
                ),
                trigger = {}
            )
        }
    }
}
