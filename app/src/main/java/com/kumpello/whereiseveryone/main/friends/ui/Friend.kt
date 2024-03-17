package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel

@Composable
fun Friend(
    modifier: Modifier = Modifier,
    friend: Friend,
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = Shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(friend.nick)
            IconButton(
                onClick = { trigger(FriendsViewModel.Command.DeleteFriend(friend.id)) },
                modifier = Modifier
                    .height(50.dp)
            ) {
                Icon(
                    modifier = Modifier.size(size = 50.dp),
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "",
                    tint = Color.Red
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    heightDp = 50
)
@Composable
fun FriendPreview() {
    WhereIsEveryoneTheme {
        Friend(
            friend = Friend(
                nick = "JanuszAndrzejNowak",
                id = "2137"
            )
        ) {}
    }
}