package com.kumpello.whereiseveryone.main.friends.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import timber.log.Timber
import com.kumpello.whereiseveryone.common.extension.isAddFriendDeepLink
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
import androidx.compose.runtime.DisposableEffect
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
import com.kumpello.whereiseveryone.main.friends.nfc.NdefHceService
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

const val TAG = "FRIENDS_SCREEN"

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

    DisposableEffect(Unit) {
        val nfcReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Timber.tag(TAG).d("Broadcast received: ${intent?.action}")
                if (intent?.action == "com.kumpello.whereiseveryone.NFC_SUCCESS") {
                    friendsViewModel.trigger(FriendsViewModel.Event.CloseNfcSharingDialog)
                    Toast.makeText(context, context?.getString(R.string.profile_shared_successfully), Toast.LENGTH_SHORT).show()
                }
            }
        }
        val filter = IntentFilter("com.kumpello.whereiseveryone.NFC_SUCCESS")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(nfcReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(nfcReceiver, filter)
        }

        onDispose {
            context.unregisterReceiver(nfcReceiver)
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

                is FriendsViewModel.Action.TriggerNfcSharing -> triggerNfcSharing(context)
                FriendsViewModel.Action.StopNfcSharing -> stopNfcSharing(context)
                is FriendsViewModel.Action.TriggerNfcReading -> triggerNfcReading(context, friendsViewModel)
                FriendsViewModel.Action.StopNfcReading -> stopNfcReading(context)
            }
        }
    }

    FriendsScreen(
        friendsViewState = friendsState,
        onFriendsEvent = friendsViewModel::trigger,
        onFriendAdded = { friendsViewModel.trigger(FriendsViewModel.Event.CheckFriends) },
        onOpenNfcReading = { friendsViewModel.trigger(FriendsViewModel.Event.OpenNfcReadingDialog) }
    )
}

@Composable
private fun FriendsScreen(
    friendsViewState: FriendsViewModel.ViewState,
    onFriendsEvent: (FriendsViewModel.Event) -> Unit,
    onFriendAdded: () -> Unit,
    onOpenNfcReading: () -> Unit,
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
        if (friendsViewState.isNfcReadingDialogOpen) {
            NfcReadingDialog(
                onDismiss = { onFriendsEvent(FriendsViewModel.Event.CloseNfcReadingDialog) }
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
                                AddFriendContent(
                                    onFriendAdded = onFriendAdded,
                                    onOpenNfcReading = onOpenNfcReading
                                )
                            } else {
                                ShareProfileContent(
                                    onShowQr = { onFriendsEvent(FriendsViewModel.Event.OpenShareDialog) },
                                    onTriggerNfc = { onFriendsEvent(FriendsViewModel.Event.OpenNfcSharingDialog) }
                                )
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

private fun processNdefMessage(message: NdefMessage?, context: Context, viewModel: FriendsViewModel) {
    var parsedUri: android.net.Uri? = null
    message?.records?.forEachIndexed { index, record ->
        Timber.tag(TAG).d("Record #$index: TNF=${record.tnf}, Type=${record.type.joinToString("") { "%02X".format(it) }}, PayloadLen=${record.payload.size}")
        if (parsedUri == null) {
            try {
                parsedUri = record.toUri()
                if (parsedUri != null) Timber.tag(TAG).d("Record #$index parsed as URI: $parsedUri")
            } catch (e: Exception) {
                Timber.tag(TAG).d("Record #$index is not a URI")
            }
        }
    }

    val uri = parsedUri
    Timber.tag(TAG).d("Final Parsed URI: $uri")

    if (uri.isAddFriendDeepLink()) {
        (context as Activity).runOnUiThread {
            Timber.tag(TAG).i("Valid Friend URI received via NFC!")
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))

            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(context.packageName)
            })
            Timber.tag(TAG).d("Triggering CloseNfcReadingDialog")
            viewModel.trigger(FriendsViewModel.Event.CloseNfcReadingDialog)
        }
    }
}

private fun triggerNfcSharing(context: Context) {
    Timber.tag(TAG).d("Triggering NFC Sharing")
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    if (nfcAdapter == null) {
        Timber.tag(TAG).e("NFC Adapter is null")
    } else {
        val cardEmulation = CardEmulation.getInstance(nfcAdapter)
        val componentName = ComponentName(context, NdefHceService::class.java)
        Timber.tag(TAG).d("Setting preferred service: $componentName")
        try {
            val success = cardEmulation.setPreferredService(context as Activity, componentName)
            Timber.tag(TAG).d("SetPreferredService success: $success")
            if (success) {
                Toast.makeText(context, context.getString(R.string.ready_to_share), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.could_not_start_sharing), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error calling setPreferredService")
        }
    }
}

private fun stopNfcSharing(context: Context) {
    Timber.tag(TAG).d("Stopping NFC Sharing")
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    if (nfcAdapter != null) {
        val cardEmulation = CardEmulation.getInstance(nfcAdapter)
        val success = cardEmulation.unsetPreferredService(context as Activity)
        Timber.tag(TAG).d("UnsetPreferredService success: $success")
    }
}

private fun triggerNfcReading(context: Context, viewModel: FriendsViewModel) {
    Timber.tag(TAG).d("Triggering NFC Reading")
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    if (nfcAdapter == null) {
        Timber.tag(TAG).e("NFC Adapter is null")
    } else {
        nfcAdapter.enableReaderMode(
            context as Activity,
            { tag ->
                val ndef = android.nfc.tech.Ndef.get(tag)
                try {
                    ndef?.connect()
                    val message = ndef?.ndefMessage

                    if (message == null || message.records.isEmpty()) {
                        Timber.tag(TAG).w("NDEF read returned empty. Attempting manual fallback...")
                        val isoDep = android.nfc.tech.IsoDep.get(tag)
                        if (isoDep != null) {
                            isoDep.connect()
                            // SELECT NDEF AID
                            isoDep.transceive(byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07, 0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01))
                            // SELECT NDEF File (E104)
                            isoDep.transceive(byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x04.toByte()))
                            // READ Length
                            val lenResp = isoDep.transceive(byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x02))
                            if (lenResp.size >= 2) {
                                val ndefLen = ((lenResp[0].toInt() and 0xFF) shl 8) or (lenResp[1].toInt() and 0xFF)
                                // READ Payload
                                val payloadResp = isoDep.transceive(byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x02, (ndefLen and 0xFF).toByte()))
                                if (payloadResp.size >= 2) {
                                    val rawNdef = payloadResp.sliceArray(0 until payloadResp.size - 2)
                                    processNdefMessage(NdefMessage(rawNdef), context, viewModel)
                                }
                            }
                            isoDep.close()
                        }
                    } else {
                        processNdefMessage(message, context, viewModel)
                    }
                    ndef?.close()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error reading NDEF tag")
                }
            },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
            null
        )
    }
}

private fun stopNfcReading(context: Context) {
    Timber.tag(TAG).d("Stopping NFC Reading")
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    nfcAdapter?.disableReaderMode(context as Activity)
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
                            speed = 0f,
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
                        speed = 0f,
                        lastUpdateTime = "12:34:56 20.04.2137",
                        lastUpdateAge = LastUpdateAge.SOMEWHAT_NEW,
                    )
                ),
                actionState = AsyncState.Idle,
                isShareDialogOpen = false,
                isNfcSharingDialogOpen = false,
                isNfcReadingDialogOpen = false,
                username = "Janusz",
                friendUsername = "Janusz"
            ),
            onFriendsEvent = {},
            onFriendAdded = {},
            onOpenNfcReading = {},
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
                            speed = 0f,
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
                        speed = 0f,
                        lastUpdateTime = "12:34:56 20.04.2137",
                        lastUpdateAge = LastUpdateAge.SOMEWHAT_NEW,
                    )
                ),
                actionState = AsyncState.Idle,
                isShareDialogOpen = false,
                isNfcSharingDialogOpen = false,
                isNfcReadingDialogOpen = false,
                username = "Janusz",
                friendUsername = "Janusz"
            ),
            onFriendsEvent = {},
            onFriendAdded = {},
            onOpenNfcReading = {},
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
                            speed = 0f,
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
                            speed = 0f,
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
                            speed = 0f,
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
                isNfcReadingDialogOpen = false,
                username = "Janusz",
                friendUsername = "Janusz"
            ),
            onFriendsEvent = {},
            onFriendAdded = {},
            onOpenNfcReading = {},
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
                            speed = 0f,
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
                            speed = 0f,
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
                            speed = 0f,
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
                isNfcReadingDialogOpen = false,
                username = "Janusz",
                friendUsername = "Janusz"
            ),
            onFriendsEvent = {},
            onFriendAdded = {},
            onOpenNfcReading = {},
        )
    }
}
