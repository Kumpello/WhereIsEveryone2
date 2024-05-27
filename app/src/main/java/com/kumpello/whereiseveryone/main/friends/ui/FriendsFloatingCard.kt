package com.kumpello.whereiseveryone.main.friends.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.shortToast
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel.DeleteFriendDialogState
import com.kumpello.whereiseveryone.main.map.ui.FloatingCard
import org.koin.androidx.compose.getViewModel

@Composable
fun FriendsFloatingCard(
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = getViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsState()

    LaunchedEffect(viewModel.action) {
        viewModel.action.collect { action ->
            when (action) {
                is FriendsViewModel.Action.AddFriendResult -> Toast.makeText(
                    context,
                    when(action.success) {
                        true -> context.getString(R.string.friend_added_successfully)
                        false -> context.getString(R.string.error_adding_friend)
                    },
                    Toast.LENGTH_SHORT
                ).show()
                is FriendsViewModel.Action.DeleteFriendResult -> shortToast(
                    context,
                    when(action.success) {
                        true -> context.getString(R.string.friend_deleted_successfully)
                        false -> context.getString(R.string.error_deleting_friend)
                    }
                )
            }
        }
    }

    FriendsFloatingCard(
        modifier = modifier,
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
private fun FriendsFloatingCard(
    modifier: Modifier = Modifier,
    viewState: FriendsViewModel.ViewState,
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    if (viewState.deleteFriendDialogState is DeleteFriendDialogState.Open) {
        DeleteFriendDialog(
            friend = viewState.deleteFriendDialogState.friend,
            trigger = trigger
        )
    }
    FloatingCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = Shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextField( //TODO: Make it more funky, maybe other composable? To consider in all Textfields
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Your friends nick") },
                        value = viewState.addFriendNick,
                        onValueChange = { nick ->
                            trigger(FriendsViewModel.Command.SetAddFriendNick(nick))
                        })
                    Spacer(Modifier.size(20.dp))
                    Button.Animated(
                        text = stringResource(R.string.add_friend),
                        width = 250
                    ) {
                        trigger(FriendsViewModel.Command.AddFriend)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(viewState.friends) { friend ->
                    Friend(
                        friend = friend,
                        trigger = trigger
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPreview() { //TODO: Get this preview unfucked
    WhereIsEveryoneTheme(darkTheme = true) {
        FriendsFloatingCard(
            viewState = FriendsViewModel.ViewState(
                friends = listOf(
                    Friend(
                        nick = "Buddy",
                        //id = "2137"
                    )
                ),
                addFriendNick = "Papator2000",
                deleteFriendDialogState = DeleteFriendDialogState.Closed
            )
        ) {}
    }
}