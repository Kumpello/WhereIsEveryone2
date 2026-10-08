package com.kumpello.whereiseveryone.feature.main.ui.friends

import com.kumpello.whereiseveryone.feature.main.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.feature.main.ui.model.AccuracyLevel
import com.kumpello.whereiseveryone.feature.main.ui.model.AltDifference
import com.kumpello.whereiseveryone.feature.main.ui.model.Friend
import com.kumpello.whereiseveryone.data.model.FriendState.ACCEPTED
import com.kumpello.whereiseveryone.data.model.FriendState.PENDING_INCOMING
import com.kumpello.whereiseveryone.data.model.FriendState.PENDING_OUTGOING
import com.kumpello.whereiseveryone.feature.main.ui.model.LastUpdateAge
import com.kumpello.whereiseveryone.feature.main.ui.model.Location

@Composable
fun Friend(modifier: Modifier = Modifier, friend: Friend, trigger: (FriendsViewModel.Event) -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("friend_card_${friend.username}")
            .clip(MaterialTheme.shapes.large)
            .clickable(
                enabled = friend.state == ACCEPTED,
                onClickLabel = stringResource(R.string.show_friend_details)
            ) { trigger(FriendsViewModel.Event.SelectFriend(friend)) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Surface(
                modifier = Modifier.size(AppSize.avatar).clearAndSetSemantics {},
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text(friend.username.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(friend.username, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val supportingText = if (friend.isPaused) stringResource(R.string.sharing_paused) else friend.formattedDistance
                supportingText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (friend.state) {
                    ACCEPTED -> FriendAction(
                        tag = "delete_button",
                        icon = Icons.Outlined.Delete,
                        description = stringResource(R.string.delete_friend_cd, friend.username),
                        destructive = true
                    ) { trigger(FriendsViewModel.Event.DeleteFriend(friend.username)) }
                    PENDING_INCOMING -> {
                        FriendAction(
                            tag = "accept_button",
                            icon = Icons.Outlined.Done,
                            description = stringResource(R.string.accept_friend_cd, friend.username)
                        ) { trigger(FriendsViewModel.Event.AcceptFriend(friend.username)) }
                        FriendAction(
                            tag = "reject_button",
                            icon = Icons.Outlined.Clear,
                            description = stringResource(R.string.reject_friend_cd, friend.username),
                            destructive = true
                        ) { trigger(FriendsViewModel.Event.RejectFriend(friend.username)) }
                    }
                    PENDING_OUTGOING -> FriendAction(
                        tag = "reject_button",
                        icon = Icons.Outlined.Clear,
                        description = stringResource(R.string.reject_friend_cd, friend.username),
                        destructive = true
                    ) { trigger(FriendsViewModel.Event.RejectFriend(friend.username)) }
                }
            }
        }
    }
}

@Composable
private fun FriendAction(
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(AppSize.touchTarget).testTag(tag)) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(AppSize.icon),
            tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(
    showBackground = true,
    heightDp = 120
)
@Composable
fun FriendPreview() {
    WhereIsEveryoneTheme {
        Friend(
            friend = Friend(
                username = "JanuszAndrzejNowak",
                status = "INBA",
                state = ACCEPTED,
                location = Location(
                    lat = 0.0,
                    lon = 0.0,
                    bearing = 0.0f,
                    alt = AltDifference.WAY_HIGHER,
                    accuracy = AccuracyLevel.PERFECT,
                    lastUpdateTime = "20.04.2137",
                    lastUpdateAge = LastUpdateAge.FRESH,
                    rawAlt = 0.0,
                    rawAccuracy = 0.0f,
                    speed = 0f,
                ),
            )
        ) {}
    }
}

@Preview(
    showBackground = true,
    heightDp = 120
)
@Composable
fun FriendOutgoingPreview() {
    WhereIsEveryoneTheme {
        Friend(
            friend = Friend(
                username = "JanuszAndrzejNowak",
                status = "INBA",
                state = PENDING_OUTGOING,
                location = Location(
                    lat = 0.0,
                    lon = 0.0,
                    bearing = 0.0f,
                    alt = AltDifference.WAY_HIGHER,
                    accuracy = AccuracyLevel.PERFECT,
                    lastUpdateTime = "20.04.2137",
                    lastUpdateAge = LastUpdateAge.FRESH,
                    rawAlt = 0.0,
                    rawAccuracy = 0.0f,
                    speed = 0f,
                ),
            )
        ) {}
    }
}

@Preview(
    showBackground = true,
    heightDp = 120
)
@Composable
fun FriendIncomingPreview() {
    WhereIsEveryoneTheme {
        Friend(
            friend = Friend(
                username = "JanuszAndrzejNowak",
                status = "INBA",
                state = PENDING_INCOMING,
                location = Location(
                    lat = 0.0,
                    lon = 0.0,
                    bearing = 0.0f,
                    alt = AltDifference.WAY_HIGHER,
                    accuracy = AccuracyLevel.PERFECT,
                    lastUpdateTime = "20.04.2137",
                    lastUpdateAge = LastUpdateAge.FRESH,
                    rawAlt = 0.0,
                    rawAccuracy = 0.0f,
                    speed = 0f,
                ),
            )
        ) {}
    }
}
