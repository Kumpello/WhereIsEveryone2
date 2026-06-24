package com.kumpello.whereiseveryone.main.friends.ui

import android.nfc.NfcAdapter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.common.ui.TextField
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.ui.FriendDetailsCard
import com.kumpello.whereiseveryone.main.friends.entity.FriendsTabItem
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel.DeleteFriendDialogState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun FriendsScreen(
    navController: NavController,
    viewModel: FriendsViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focusManager = LocalFocusManager.current

    val keyboardVisible =
        WindowInsets.ime.getBottom(LocalDensity.current) > 0

    BackHandler(enabled = keyboardVisible) {
        focusManager.clearFocus()
    }

    //TODO: Add notification on server side to get rid of this
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(
            Lifecycle.State.STARTED
        ) {
            while (true) {
                viewModel.trigger(FriendsViewModel.Event.CheckFriends)

                delay(10.seconds)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                FriendsViewModel.Action.BackToMap -> navController.popBackStack()
                is FriendsViewModel.Action.Toast -> Toast.makeText(
                    context,
                    action.id,
                    Toast.LENGTH_SHORT
                )

                else -> Unit
            }
        }
    }

    FriendsScreen(
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
private fun FriendsScreen(
    viewState: FriendsViewModel.ViewState,
    trigger: (FriendsViewModel.Event) -> Unit,
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (viewState.deleteFriendDialogState is DeleteFriendDialogState.Open) {
            DeleteFriendDialog(
                friend = viewState.deleteFriendDialogState.friend,
                trigger = trigger
            )
        }
        if (viewState.isShareDialogOpen) {
            QrCodeDialog(
                username = viewState.username,
                onDismiss = { trigger(FriendsViewModel.Event.CloseShareDialog) }
            )
        }
        viewState.selectedFriend?.let { friend ->
            FriendDetailsCard(
                friend = friend,
                onDismiss = { trigger(FriendsViewModel.Event.ClearSelectedFriend) },
                onNavigate = { _ ->
                    trigger(FriendsViewModel.Event.ClearSelectedFriend)
                }
            )
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(100f),
            visible = viewState.actionState is AsyncState.Loading,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            val message = (viewState.actionState as? AsyncState.Loading)?.let {
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
                        .fillMaxWidth(),
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
                    Column(
                        modifier = Modifier
                            .padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TextField.Regular(
                            label = stringResource(R.string.your_friends_nick),
                            value = viewState.addFriendNick,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                            ),
                            onValueChange = { nick ->
                                trigger(FriendsViewModel.Event.SetAddFriendNick(nick))
                            }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { trigger(FriendsViewModel.Event.OpenShareDialog) },
                                enabled = !viewState.actionState.isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Show My QR",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                                    when {
                                        nfcAdapter == null -> trigger(FriendsViewModel.Event.OnNfcNotSupported)
                                        !nfcAdapter.isEnabled -> trigger(FriendsViewModel.Event.OnNfcDisabled)
                                        else -> trigger(FriendsViewModel.Event.ShareViaNfc)
                                    }
                                },
                                enabled = !viewState.actionState.isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contactless,
                                    contentDescription = "NFC Share",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Button.Animated(
                                text = stringResource(R.string.add_friend),
                                width = 150,
                                enabled = !viewState.actionState.isLoading
                            ) {
                                trigger(FriendsViewModel.Event.AddFriend)
                            }
                        }
                    }
                }
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
                            Tab( //TODO Customize!
                                selected = index == selectedTabItem,
                                onClick = {
                                    selectedTabItem = index
                                },
                                text = {
                                    when (index) {
                                        1 if viewState.friends.any { friend -> friend.state == FriendState.PENDING_INCOMING } -> {
                                            Text(
                                                modifier = Modifier.alpha(alphaTransitionOnTab),
                                                text = tabItem.name
                                            )
                                        }

                                        2 if viewState.friends.any { friend -> friend.state == FriendState.PENDING_OUTGOING } -> {
                                            Text(
                                                modifier = Modifier.alpha(alphaTransitionOnTab),
                                                text = tabItem.name
                                            )
                                        }

                                        else -> Text(
                                            modifier = Modifier.alpha(0.85f),
                                            text = tabItem.name
                                        )
                                    }
                                })
                        }
                    }

                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        state = pagerState,
                        verticalAlignment = Alignment.Top
                    ) {
                        when (selectedTabItem) {
                            0 -> FriendsCategory(
                                friends = viewState.friends.filter { friend -> friend.state == FriendState.ACCEPTED },
                                trigger = trigger
                            )

                            1 -> FriendsCategory(
                                friends = viewState.friends.filter { friend -> friend.state == FriendState.PENDING_INCOMING },
                                trigger = trigger
                            )

                            2 -> FriendsCategory(
                                friends = viewState.friends.filter { friend -> friend.state == FriendState.PENDING_OUTGOING },
                                trigger = trigger
                            )

                        }
                    }

                }
            }
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

@Composable
fun QrCodeDialog(
    username: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = Shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your QR Code",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                val qrContent = "whereiseveryone://addfriend/$username"
                val qrBitmap = remember(qrContent) {
                    QrCodeGenerator.generateQrCode(qrContent, 512)
                }

                qrBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                } ?: Text("Error generating QR")

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scan this to add me as a friend",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button.Animated(text = "Close", width = 150) {
                    onDismiss()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsWithDetailsPreview() {
    WhereIsEveryoneTheme(false) {
        FriendsScreen(
            viewState = FriendsViewModel.ViewState(
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
                addFriendNick = "Papator2000",
                deleteFriendDialogState = DeleteFriendDialogState.Closed,
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
                username = "Janusz"
            )
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsWithDetailsPreviewDark() {
    WhereIsEveryoneTheme(true) {
        FriendsScreen(
            viewState = FriendsViewModel.ViewState(
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
                addFriendNick = "Papator2000",
                deleteFriendDialogState = DeleteFriendDialogState.Closed,
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
                username = "Janusz"
            )
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPreview() {
    WhereIsEveryoneTheme(false) {
        FriendsScreen(
            viewState = FriendsViewModel.ViewState(
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
                addFriendNick = "Papator2000",
                deleteFriendDialogState = DeleteFriendDialogState.Closed,
                selectedFriend = null,
                actionState = AsyncState.Idle,
                isShareDialogOpen = false,
                username = "Janusz"
            )
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPreviewDark() {
    WhereIsEveryoneTheme(true) {
        FriendsScreen(
            viewState = FriendsViewModel.ViewState(
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
                addFriendNick = "Papator2000",
                deleteFriendDialogState = DeleteFriendDialogState.Closed,
                selectedFriend = null,
                actionState = AsyncState.Idle,
                isShareDialogOpen = false,
                username = "Janusz"
            )
        ) {}
    }
}
