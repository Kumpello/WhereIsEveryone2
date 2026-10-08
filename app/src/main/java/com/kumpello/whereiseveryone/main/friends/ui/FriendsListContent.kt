package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.theme.AppSize
import com.kumpello.whereiseveryone.common.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FriendsListContent(viewModel: FriendsViewModel = koinViewModel()) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    FriendsListContent(viewState = viewState, onEvent = viewModel::trigger)
}

@Composable
fun FriendsListContent(
    viewState: FriendsViewModel.ViewState,
    onEvent: (FriendsViewModel.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(FriendState.ACCEPTED, FriendState.PENDING_INCOMING, FriendState.PENDING_OUTGOING)
    val titles = listOf(
        stringResource(R.string.friends),
        stringResource(R.string.friends_incoming_tab),
        stringResource(R.string.friends_outgoing_tab)
    )
    val groups = remember(viewState.friends) { viewState.friends.groupBy { it.state } }
    val pagerState = rememberPagerState { categories.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(titles[index], style = MaterialTheme.typography.labelLarge)
                            val count = groups[category].orEmpty().size
                            if (count > 0 && category != FriendState.ACCEPTED) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) { Text(count.toString()) }
                            }
                        }
                    }
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).testTag("friends_pager"), verticalAlignment = Alignment.Top) { page ->
            val category = categories[page]
            val friends = groups[category].orEmpty()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = AppSpacing.md, bottom = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                if (friends.isEmpty()) {
                    item(key = "empty") { EmptyFriends(category) }
                }
                items(friends, key = { it.username }) { friend -> Friend(friend = friend, trigger = onEvent) }
            }
        }
    }
}

@Composable
private fun EmptyFriends(category: FriendState) {
    val titleId = when (category) {
        FriendState.ACCEPTED -> R.string.friends_empty_title
        FriendState.PENDING_INCOMING -> R.string.incoming_empty_title
        FriendState.PENDING_OUTGOING -> R.string.outgoing_empty_title
    }
    val messageId = when (category) {
        FriendState.ACCEPTED -> R.string.friends_empty_message
        FriendState.PENDING_INCOMING -> R.string.incoming_empty_message
        FriendState.PENDING_OUTGOING -> R.string.outgoing_empty_message
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md, vertical = AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.PeopleOutline,
            contentDescription = null,
            modifier = Modifier.size(AppSize.avatar),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(stringResource(titleId), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(messageId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsListContentPreview() {
    WhereIsEveryoneTheme {
        FriendsListContent(
            viewState = FriendsViewModel.ViewState(
                friends = listOf(),
                deleteFriendDialogState = FriendsViewModel.DeleteFriendDialogState.Closed,
                selectedFriend = null,
                actionState = com.kumpello.whereiseveryone.common.presentation.AsyncState.Idle,
                isShareDialogOpen = false,
                isNfcSharingDialogOpen = false,
                isNfcReadingDialogOpen = false,
                username = "Janusz",
                friendUsername = "Janusz"
            ),
            onEvent = {}
        )
    }
}
