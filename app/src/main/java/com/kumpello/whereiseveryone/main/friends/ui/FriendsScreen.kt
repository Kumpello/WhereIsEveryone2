package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainNavGraph
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@MainNavGraph(start = false)
@Destination
@Composable
fun FriendsScreen(
    navigator: DestinationsNavigator,
    viewModel: FriendsViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {

    }

    FriendsScreen(
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
fun FriendsScreen(
    viewState: FriendsViewModel.ViewState,
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {

            TextField(
                label = { Text(text = "Your friends nick") },
                value = viewState.addFriendNick,
                onValueChange = { nick ->
                    trigger(FriendsViewModel.Command.SetAddFriendNick(nick))
                })
            Spacer(Modifier.size(20.dp))
            AddFriendButton(viewState, trigger)
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            state = rememberLazyListState(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(viewState.friends) { friend ->
                Friend(friend.nick)
            }
        }
    }
}

//TODO: Refactor
@Composable
fun AddFriendButton(
    viewState: FriendsViewModel.ViewState,
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    Button(
        onClick = {
            //viewModel.onEvent(UIEvent.AddFriend(mContext.applicationContext as WhereIsEveryoneApplication, nick))
        },
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier
            .height(50.dp)
    ) {
        Text(text = "Add friend")
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPreview() {
    WhereIsEveryoneTheme {
        //Friends(rememberNavController(), MapViewModel())
    }
}