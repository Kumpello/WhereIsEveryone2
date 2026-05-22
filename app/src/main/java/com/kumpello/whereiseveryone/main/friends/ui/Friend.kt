package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.FriendState.*
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel

@Composable
fun Friend(
    modifier: Modifier = Modifier,
    friend: Friend,
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    //TODO Add colors!
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = Shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                text = friend.username
            ) //TODO: Add friend since
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                when (friend.state) {
                    ACCEPTED -> AcceptedButtons(friend, trigger)
                    PENDING_INCOMING -> PendingIncomingButtons(friend, trigger)
                    PENDING_OUTGOING -> PendingOutgoingButtons(friend, trigger)
                }
            }
        }
    }
}

@Composable
private fun AcceptedButtons(
    friend: Friend,
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    IconButton( //TODO: Add share location switch
        onClick = { trigger(FriendsViewModel.Command.DeleteFriend(friend.username)) },
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

@Composable
private fun PendingIncomingButtons(
    friend: Friend,
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    IconButton(
        onClick = { trigger(FriendsViewModel.Command.AcceptFriend(friend.username)) },
        modifier = Modifier
            .height(50.dp)
    ) {
        Icon(
            modifier = Modifier.size(size = 50.dp),
            imageVector = Icons.Outlined.Done,
            contentDescription = "",
            tint = Color.Green
        )
    }
    IconButton(
        onClick = { trigger(FriendsViewModel.Command.RejectFriend(friend.username)) },
        modifier = Modifier
            .height(50.dp)
    ) {
        Icon(
            modifier = Modifier.size(size = 50.dp),
            imageVector = Icons.Outlined.Clear,
            contentDescription = "",
            tint = Color.Red
        )
    }
}

@Composable
private fun PendingOutgoingButtons(
    friend: Friend,
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    IconButton(
        onClick = { trigger(FriendsViewModel.Command.RejectFriend(friend.username)) },
        modifier = Modifier
            .height(50.dp)
    ) {
        Icon(
            modifier = Modifier.size(size = 50.dp),
            imageVector = Icons.Outlined.Clear,
            contentDescription = "",
            tint = Color.Red
        )
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
                username = "JanuszAndrzejNowak",
                status = "INBA",
                state = FriendState.ACCEPTED,
                location = Location(
                    lat = 0.0,
                    lon = 0.0,
                    bearing = 0.0f,
                    alt = AltDifference.WAY_HIGHER,
                    accuracy = AccuracyLevel.PERFECT,
                    lastUpdateTime = "20.04.2137",
                    lastUpdateAge = LastUpdateAge.FRESH,
                ),
            )
        ) {}
    }
}