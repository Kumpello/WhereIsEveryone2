package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.friends.entity.FriendsTabItem
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FriendsListContent(
    viewModel: FriendsViewModel = koinViewModel()
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    FriendsListContent(
        viewState = viewState,
        onEvent = viewModel::trigger
    )
}

@Composable
fun FriendsListContent(
    viewState: FriendsViewModel.ViewState,
    onEvent: (FriendsViewModel.Event) -> Unit
) {
    val listTabItem = listOf(
        FriendsTabItem(stringResource(R.string.friends), "FriendsTab"),
        FriendsTabItem(stringResource(R.string.incoming_requests), "IncomingTab"),
        FriendsTabItem(stringResource(R.string.outgoing_requests), "OutgoingTab")
    )
    var selectedTabItem by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0) { listTabItem.size }
    val alphaTransitionOnTab by rememberInfiniteTransition().animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1000, easing = LinearEasing),
            RepeatMode.Restart
        )
    )

    LaunchedEffect(pagerState.currentPage) {
        selectedTabItem = pagerState.currentPage
    }

    Column(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(Shapes.large)
            .fillMaxSize()
    ) {
        PrimaryTabRow(selectedTabItem) {
            listTabItem.forEachIndexed { index, tabItem ->
                Tab(
                    selected = index == selectedTabItem,
                    onClick = {
                        selectedTabItem = index
                    },
                    text = {
                        val isHighlighted = when (index) {
                            1 -> viewState.friends.any { friend -> friend.state == FriendState.PENDING_INCOMING }
                            2 -> viewState.friends.any { friend -> friend.state == FriendState.PENDING_OUTGOING }
                            else -> false
                        }
                        
                        Text(
                            modifier = Modifier.alpha(if (isHighlighted) alphaTransitionOnTab else 0.85f),
                            text = tabItem.name
                        )
                    })
            }
        }

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            verticalAlignment = Alignment.Top
        ) {
            val friendsToDisplay = when (selectedTabItem) {
                0 -> viewState.friends.filter { friend -> friend.state == FriendState.ACCEPTED }
                1 -> viewState.friends.filter { friend -> friend.state == FriendState.PENDING_INCOMING }
                2 -> viewState.friends.filter { friend -> friend.state == FriendState.PENDING_OUTGOING }
                else -> emptyList()
            }
            FriendsCategory(
                friends = friendsToDisplay,
                trigger = onEvent
            )
        }
    }
}

@Composable
private fun FriendsCategory(
    modifier: Modifier = Modifier,
    friends: List<Friend>,
    trigger: (FriendsViewModel.Event) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentPadding = PaddingValues(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(friends) { friend ->
            Friend(
                friend = friend,
                trigger = trigger
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsListContentPreview() {
    FriendsListContent(
        viewState = FriendsViewModel.ViewState(
            friends = listOf(),
            deleteFriendDialogState = FriendsViewModel.DeleteFriendDialogState.Closed,
            selectedFriend = null,
            actionState = com.kumpello.whereiseveryone.common.presentation.AsyncState.Idle,
            isShareDialogOpen = false,
            username = "Janusz",
            friendUsername = "Janusz"
        ),
        onEvent = {}
    )
}
