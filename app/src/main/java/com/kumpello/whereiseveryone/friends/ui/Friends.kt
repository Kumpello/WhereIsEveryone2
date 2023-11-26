package com.kumpello.whereiseveryone.friends.ui

import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.map.data.model.UserPosition
import com.kumpello.whereiseveryone.map.domain.events.GetPositionsEvent
import com.kumpello.whereiseveryone.map.presentation.MainActivityViewModel
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun Friends(
    navigator: DestinationsNavigator,
    viewModel: MainActivityViewModel
) {
    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val application = mContext.applicationContext as WhereIsEveryoneApplication

    var friends: List<UserPosition> = listOf()

    LaunchedEffect(mContext) {
        viewModel.event.collect { event ->
            when (event) {
                is GetPositionsEvent.GetError -> Toast
                    .makeText(mContext, event.error.message, Toast.LENGTH_SHORT)
                    .show()

                is GetPositionsEvent.GetSuccess -> friends = event.organizationsData.positions
            }
        }
    }

    Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            val nick = remember { mutableStateOf(TextFieldValue()) }

            TextField(
                label = { Text(text = "Your friends nick") },
                value = nick.value,
                onValueChange = { nick.value = it })
            Spacer(Modifier.size(20.dp))
            AddFriendButton(viewModel, nick.value.text)
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            state = rememberLazyListState(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(friends) { friend ->
                Friend(friend.nick)
            }
        }
    }
}

@Composable
fun AddFriendButton(viewModel: MainActivityViewModel, nick : String) {
    val mContext = LocalContext.current

    Button(
        onClick = {
            viewModel.onEvent(UIEvent.AddFriend(mContext.applicationContext as WhereIsEveryoneApplication, nick))
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
        Friends(rememberNavController(), MainActivityViewModel())
    }
}