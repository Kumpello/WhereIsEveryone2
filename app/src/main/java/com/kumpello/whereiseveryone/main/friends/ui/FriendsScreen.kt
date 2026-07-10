package com.kumpello.whereiseveryone.main.friends.ui

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.ui.FriendDetailsCard
import com.kumpello.whereiseveryone.main.friends.presentation.AddFriendViewModel
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.friends.presentation.ShareProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@Composable
fun FriendsScreen(
    navController: NavController,
    friendsViewModel: FriendsViewModel = koinViewModel(),
) {
    val friendsState by friendsViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focusManager = LocalFocusManager.current

    val keyboardVisible =
        WindowInsets.ime.getBottom(LocalDensity.current) > 0

    BackHandler(enabled = keyboardVisible) {
        focusManager.clearFocus()
    }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                friendsViewModel.trigger(FriendsViewModel.Event.CheckFriends)
                delay(10.seconds)
            }
        }
    }

    LaunchedEffect(Unit) {
        friendsViewModel.action.collect { action ->
            when (action) {
                FriendsViewModel.Action.BackToMap -> navController.popBackStack()
                is FriendsViewModel.Action.Toast -> Toast.makeText(
                    context,
                    action.id,
                    Toast.LENGTH_SHORT
                ).show()

                is FriendsViewModel.Action.TriggerNfcSharing -> {
                    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                    val uri = "whereiseveryone://addfriend/${action.username}"
                    val message = NdefMessage(
                        arrayOf(
                            NdefRecord.createUri(uri),
                            NdefRecord.createApplicationRecord(context.packageName)
                        )
                    )
                    try {
                        val method = nfcAdapter?.javaClass?.getMethod(
                            "setNdefPushMessage",
                            NdefMessage::class.java,
                            Activity::class.java
                        )
                        method?.invoke(nfcAdapter, message, context as Activity)
                        Toast.makeText(
                            context,
                            "NFC Sharing enabled for ${action.username}. Bring devices together.",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Timber.tag("FRIENDS_SCREEN").e(e, "Error setting NDEF push message")
                        Toast.makeText(
                            context,
                            "NFC Sharing not supported on this device/version",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                FriendsViewModel.Action.StopNfcSharing -> {
                    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                    try {
                        val method = nfcAdapter?.javaClass?.getMethod(
                            "setNdefPushMessage",
                            NdefMessage::class.java,
                            Activity::class.java
                        )
                        method?.invoke(nfcAdapter, null, context as Activity)
                    } catch (e: Exception) {
                        Timber.tag("FRIENDS_SCREEN").e(e, "Error stopping NDEF push message")
                    }
                }
            }
        }
    }

    FriendsScreen(
        friendsViewState = friendsState,
        onFriendsEvent = friendsViewModel::trigger,
        onFriendAdded = { friendsViewModel.trigger(FriendsViewModel.Event.CheckFriends) }
    )
}

@Composable
private fun FriendsScreen(
    friendsViewState: FriendsViewModel.ViewState,
    onFriendsEvent: (FriendsViewModel.Event) -> Unit,
    onFriendAdded: () -> Unit,
    addFriendContent: @Composable () -> Unit = { AddFriendContent(onFriendAdded = onFriendAdded) },
    shareProfileContent: @Composable () -> Unit = {
        ShareProfileContent(
            onShowQr = { onFriendsEvent(FriendsViewModel.Event.OpenShareDialog) },
            onTriggerNfc = { onFriendsEvent(FriendsViewModel.Event.OpenNfcSharingDialog) }
        )
    }
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (friendsViewState.deleteFriendDialogState is FriendsViewModel.DeleteFriendDialogState.Open) {
            DeleteFriendDialog(
                friend = friendsViewState.deleteFriendDialogState.friend,
                trigger = onFriendsEvent
            )
        }
        if (friendsViewState.isShareDialogOpen) {
            QrCodeDialog(
                username = friendsViewState.username,
                onDismiss = { onFriendsEvent(FriendsViewModel.Event.CloseShareDialog) }
            )
        }
        if (friendsViewState.isNfcSharingDialogOpen) {
            NfcSharingDialog(
                onDismiss = { onFriendsEvent(FriendsViewModel.Event.CloseNfcSharingDialog) }
            )
        }
        friendsViewState.selectedFriend?.let { friend ->
            FriendDetailsCard(
                friend = friend,
                onDismiss = { onFriendsEvent(FriendsViewModel.Event.ClearSelectedFriend) },
                onNavigate = { _ ->
                    onFriendsEvent(FriendsViewModel.Event.ClearSelectedFriend)
                },
                onSharingToggle = {
                    onFriendsEvent(FriendsViewModel.Event.ToggleSharing(it.username))
                }
            )
        }

        val actionState = friendsViewState.actionState.takeIf { it !is AsyncState.Idle } 

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(100f),
            visible = actionState is AsyncState.Loading,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            val message = (actionState as? AsyncState.Loading)?.let {
                it.messageId?.let { id -> stringResource(id) } ?: it.message
            } ?: ""
            Card(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.8f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = Shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp,
                    ),
                    colors = CardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.onErrorContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = Shapes.large,
                ) {
                    val topPagerState = rememberPagerState(initialPage = 0) { 2 }
                    val coroutineScope = rememberCoroutineScope()
                    val topTabItems = listOf(
                        stringResource(R.string.add_friend),
                        stringResource(R.string.share_profile)
                    )
                    Column(modifier = Modifier.fillMaxSize()) {
                        PrimaryTabRow(
                            selectedTabIndex = topPagerState.currentPage,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            topTabItems.forEachIndexed { index, title ->
                                Tab(
                                    selected = topPagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            topPagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                )
                            }
                        }
                        HorizontalPager(
                            state = topPagerState,
                            modifier = Modifier.weight(1f)
                        ) { page ->
                            if (page == 0) {
                                addFriendContent()
                            } else {
                                shareProfileContent()
                            }
                        }
                    }
                }
                
                FriendsListContent(
                    onEvent = onFriendsEvent,
                    viewState = friendsViewState
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsWithDetailsPreview() {
    WhereIsEveryoneTheme(false) {
        FriendsScreen(
            friendsViewState = FriendsViewModel.ViewState(
                friends = listOf(
                    Friend(
                        username = "JanuszAndrzejNowak",
                        status = "INBA",
                        state = FriendState.ACCEPTED,
                        location = Location(
                            lat = 0.0,
                            lon = 0.0,
                            bearing = 0.0f,
                            alt = AltDifference.SOMEWHAT_SAME,
                            rawAlt = 0.0,
                            accuracy = AccuracyLevel.MEDIUM,
                            rawAccuracy = 15.0f,
                            lastUpdateTime = "12:34:56 20.04.2137",
                            lastUpdateAge = LastUpdateAge.SOMEWHAT_NEW,
                        )
                    )
                ),
                deleteFriendDialogState = FriendsViewModel.DeleteFriendDialogState.Closed,
                selectedFriend = Friend(
                    username = "JanuszAndrzejNowak",
                    status = "INBA",
                    state = FriendState.ACCEPTED,
                    location = Location(
                        lat = 0.0,
                        lon = 0.0,
                        bearing = 0.0f,
                        alt = AltDifference.SOMEWHAT_SAME,
                        rawAlt = 0.0,
                        accuracy = AccuracyLevel.MEDIUM,
                        rawAccuracy = 15.0f,
                        lastUpdateTime = "12:34:56 20.04.2137",
                        lastUpdateAge = LastUpdateAge.SOMEWHAT_NEW,
                    )
                ),
                actionState = AsyncState.Idle,
                isShareDialogOpen = false,
                isNfcSharingDialogOpen = false,
                username = "Janusz",
                friendUsername = "Janusz"
            ),
            onFriendsEvent = {},
            onFriendAdded = {},
            addFriendContent = { 
                AddFriendContent(
                    viewState = AddFriendViewModel.ViewState(
                        addFriendNick = "Papator2000",
                        actionState = AsyncState.Idle
                    ),
                    onEvent = {}
                )
            },
            shareProfileContent = {
                ShareProfileContent(
                    viewState = ShareProfileViewModel.ViewState(
                        username = "Janusz"
                    ),
                    onEvent = {},
                    onShowQr = {},
                    onTriggerNfc = {}
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsWithDetailsPreviewDark() {
    WhereIsEveryoneTheme(true) {
        FriendsScreen(
            friendsViewState = FriendsViewModel.ViewState(
                friends = listOf(
                    Friend(
                        username = "JanuszAndrzejNowak",
                        status = "INBA",
                        state = FriendState.ACCEPTED,
                        location = Location(
                            lat = 0.0,
                            lon = 0.0,
                            bearing = 0.0f,
                            alt = AltDifference.SOMEWHAT_SAME,
                            rawAlt = 0.0,
                            accuracy = AccuracyLevel.MEDIUM,
                            rawAccuracy = 15.0f,
                            lastUpdateTime = "12:34:56 20.04.2137",
                            lastUpdateAge = LastUpdateAge.SOMEWHAT_NEW,
                        )
                    )
                ),
                deleteFriendDialogState = FriendsViewModel.DeleteFriendDialogState.Closed,
                selectedFriend = Friend(
                    username = "JanuszAndrzejNowak",
                    status = "INBA",
                    state = FriendState.ACCEPTED,
                    location = Location(
                        lat = 0.0,
                        lon = 0.0,
                        bearing = 0.0f,
                        alt = AltDifference.SOMEWHAT_SAME,
                        rawAlt = 0.0,
                        accuracy = AccuracyLevel.MEDIUM,
                        rawAccuracy = 15.0f,
                        lastUpdateTime = "12:34:56 20.04.2137",
                        lastUpdateAge = LastUpdateAge.SOMEWHAT_NEW,
                    )
                ),
                actionState = AsyncState.Idle,
                isShareDialogOpen = false,
                isNfcSharingDialogOpen = false,
                username = "Janusz",
                friendUsername = "Janusz"
            ),
            onFriendsEvent = {},
            onFriendAdded = {},
            addFriendContent = { 
                AddFriendContent(
                    viewState = AddFriendViewModel.ViewState(
                        addFriendNick = "Papator2000",
                        actionState = AsyncState.Idle
                    ),
                    onEvent = {}
                )
            },
            shareProfileContent = {
                ShareProfileContent(
                    viewState = ShareProfileViewModel.ViewState(
                        username = "Janusz"
                    ),
                    onEvent = {},
                    onShowQr = {},
                    onTriggerNfc = {}
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPreview() {
    WhereIsEveryoneTheme(false) {
        FriendsScreen(
            friendsViewState = FriendsViewModel.ViewState(
                friends = listOf(
                    Friend(
                        username = "JanuszAndrzejNowak",
                        status = "INBA",
                        state = FriendState.ACCEPTED,
                        location = Location(
                            lat = 0.0,
                            lon = 0.0,
                            bearing = 0.0f,
                            alt = AltDifference.SOMEWHAT_SAME,
                            rawAlt = 0.0,
                            accuracy = AccuracyLevel.MEDIUM,
                            rawAccuracy = 15.0f,
                            lastUpdateTime = "12:34:56 20.04.2137",
                            lastUpdateAge = LastUpdateAge.SOMEWHAT_NEW,
                        )
                    ),
                    Friend(
                        username = "Kozak",
                        status = "INBA",
                        state = FriendState.PENDING_INCOMING,
                        location = Location(
                            lat = 0.0,
                            lon = 0.0,
                            bearing = 0.0f,
                            alt = AltDifference.WAY_HIGHER,
                            rawAlt = 100.0,
                            accuracy = AccuracyLevel.PERFECT,
                            rawAccuracy = 0.0f,
                            lastUpdateTime = "12:34:56 20.04.2137",
                            lastUpdateAge = LastUpdateAge.FRESH,
                        )
                    ),
                    Friend(
                        username = "TenTrzeci",
                        status = "INBA",
                        state = FriendState.PENDING_OUTGOING,
                        location = Location(
                            lat = 0.0,
                            lon = 0.0,
                            bearing = 0.0f,
                            alt = AltDifference.WAY_LOWER,
                            rawAlt = -50.0,
                            accuracy = AccuracyLevel.TRAGIC,
                            rawAccuracy = 50.0f,
                            lastUpdateTime = "12:34:56 20.04.2137",
                            lastUpdateAge = LastUpdateAge.OLD,
                        )
                    )
                ),
                deleteFriendDialogState = FriendsViewModel.DeleteFriendDialogState.Closed,
                selectedFriend = null,
                actionState = AsyncState.Idle,
                isShareDialogOpen = false,
                isNfcSharingDialogOpen = false,
                username = "Janusz",
                friendUsername = "Janusz"
            ),
            onFriendsEvent = {},
            onFriendAdded = {},
            addFriendContent = { 
                AddFriendContent(
                    viewState = AddFriendViewModel.ViewState(
                        addFriendNick = "Papator2000",
                        actionState = AsyncState.Idle
                    ),
                    onEvent = {}
                )
            },
            shareProfileContent = {
                ShareProfileContent(
                    viewState = ShareProfileViewModel.ViewState(
                        username = "Janusz"
                    ),
                    onEvent = {},
                    onShowQr = {},
                    onTriggerNfc = {}
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPreviewDark() {
    WhereIsEveryoneTheme(true) {
        FriendsScreen(
            friendsViewState = FriendsViewModel.ViewState(
                friends = listOf(
                    Friend(
                        username = "JanuszAndrzejNowak",
                        status = "INBA",
                        state = FriendState.ACCEPTED,
                        location = Location(
                            lat = 0.0,
                            lon = 0.0,
                            bearing = 0.0f,
                            alt = AltDifference.SOMEWHAT_SAME,
                            rawAlt = 0.0,
                            accuracy = AccuracyLevel.MEDIUM,
                            rawAccuracy = 15.0f,
                            lastUpdateTime = "12:34:56 20.04.2137",
                            lastUpdateAge = LastUpdateAge.SOMEWHAT_NEW,
                        )
                    ),
                    Friend(
                        username = "Kozak",
                        status = "INBA",
                        state = FriendState.PENDING_INCOMING,
                        location = Location(
                            lat = 0.0,
                            lon = 0.0,
                            bearing = 0.0f,
                            alt = AltDifference.WAY_HIGHER,
                            rawAlt = 100.0,
                            accuracy = AccuracyLevel.PERFECT,
                            rawAccuracy = 0.0f,
                            lastUpdateTime = "12:34:56 20.04.2137",
                            lastUpdateAge = LastUpdateAge.FRESH,
                        )
                    ),
                    Friend(
                        username = "TenTrzeci",
                        status = "INBA",
                        state = FriendState.PENDING_OUTGOING,
                        location = Location(
                            lat = 0.0,
                            lon = 0.0,
                            bearing = 0.0f,
                            alt = AltDifference.WAY_LOWER,
                            rawAlt = -50.0,
                            accuracy = AccuracyLevel.TRAGIC,
                            rawAccuracy = 50.0f,
                            lastUpdateTime = "12:34:56 20.04.2137",
                            lastUpdateAge = LastUpdateAge.OLD,
                        )
                    )
                ),
                deleteFriendDialogState = FriendsViewModel.DeleteFriendDialogState.Closed,
                selectedFriend = null,
                actionState = AsyncState.Idle,
                isShareDialogOpen = false,
                isNfcSharingDialogOpen = false,
                username = "Janusz",
                friendUsername = "Janusz"
            ),
            onFriendsEvent = {},
            onFriendAdded = {},
            addFriendContent = { 
                AddFriendContent(
                    viewState = AddFriendViewModel.ViewState(
                        addFriendNick = "Papator2000",
                        actionState = AsyncState.Idle
                    ),
                    onEvent = {}
                )
            },
            shareProfileContent = {
                ShareProfileContent(
                    viewState = ShareProfileViewModel.ViewState(
                        username = "Janusz"
                    ),
                    onEvent = {},
                    onShowQr = {},
                    onTriggerNfc = {}
                )
            }
        )
    }
}
