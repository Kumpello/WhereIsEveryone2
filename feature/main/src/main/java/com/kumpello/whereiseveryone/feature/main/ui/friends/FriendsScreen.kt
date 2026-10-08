package com.kumpello.whereiseveryone.feature.main.ui.friends

import com.kumpello.whereiseveryone.feature.main.R
import android.app.Activity
import android.app.KeyguardManager
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.core.util.isAddFriendDeepLink
import com.kumpello.whereiseveryone.core.presentation.AsyncState
import com.kumpello.whereiseveryone.core.ui.components.ScreenHeader
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.core.ui.theme.Shapes
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.feature.main.ui.model.AccuracyLevel
import com.kumpello.whereiseveryone.feature.main.ui.model.AltDifference
import com.kumpello.whereiseveryone.feature.main.ui.model.Friend
import com.kumpello.whereiseveryone.data.model.FriendState
import com.kumpello.whereiseveryone.feature.main.ui.model.LastUpdateAge
import com.kumpello.whereiseveryone.feature.main.ui.model.Location
import com.kumpello.whereiseveryone.feature.main.ui.components.FriendDetailsCard
import com.kumpello.whereiseveryone.main.friends.nfc.NdefHceService
import com.kumpello.whereiseveryone.feature.main.sharing.nfc.NfcSharingSession
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber

const val TAG = "FRIENDS_SCREEN"

@Composable
fun FriendsScreen(
    navController: NavController,
    friendsViewModel: FriendsViewModel = koinViewModel(),
    addFriendViewModel: AddFriendViewModel = koinViewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    ),
) {
    val friendsState by friendsViewModel.state.collectAsStateWithLifecycle()
    val addFriendState by addFriendViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val nfcSharingSession = koinInject<NfcSharingSession>()
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

    DisposableEffect(lifecycle, context, friendsViewModel) {
        fun stopSharing() {
            nfcSharingSession.stop()
            friendsViewModel.trigger(FriendsViewModel.Event.CloseNfcSharingDialog)
            stopNfcSharing(context)
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) stopSharing()
        }
        lifecycle.addObserver(observer)
        val nfcReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Timber.tag(TAG).d("Broadcast received: ${intent?.action}")
                if (intent?.action == NdefHceService.ACTION_PROFILE_SHARED) {
                    friendsViewModel.trigger(FriendsViewModel.Event.CloseNfcSharingDialog)
                    Toast.makeText(context, context?.getString(R.string.profile_shared_successfully), Toast.LENGTH_SHORT).show()
                }
            }
        }
        val filter = IntentFilter(NdefHceService.ACTION_PROFILE_SHARED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(nfcReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(nfcReceiver, filter)
        }

        onDispose {
            stopSharing()
            lifecycle.removeObserver(observer)
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

                is FriendsViewModel.Action.TriggerNfcSharing -> {
                    if (nfcSharingSession.current() != null) {
                        val started = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                            triggerNfcSharing(context)
                        if (!started) {
                            friendsViewModel.trigger(FriendsViewModel.Event.CloseNfcSharingDialog)
                            Toast.makeText(context, R.string.could_not_start_sharing, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                FriendsViewModel.Action.StopNfcSharing -> stopNfcSharing(context)
                is FriendsViewModel.Action.TriggerNfcReading -> triggerNfcReading(context, friendsViewModel)
                FriendsViewModel.Action.StopNfcReading -> stopNfcReading(context)
            }
        }
    }

    FriendsScreen(
        friendsViewState = friendsState,
        pendingLinkedFriend = addFriendState.pendingLinkedFriend,
        onAddFriendEvent = addFriendViewModel::trigger,
        onFriendsEvent = friendsViewModel::trigger,
        onFriendAdded = { friendsViewModel.trigger(FriendsViewModel.Event.CheckFriends) },
        onOpenNfcReading = { friendsViewModel.trigger(FriendsViewModel.Event.OpenNfcReadingDialog) },
        onBack = { navController.popBackStack() }
    )
}

@Composable
private fun FriendsScreen(
    friendsViewState: FriendsViewModel.ViewState,
    onFriendsEvent: (FriendsViewModel.Event) -> Unit,
    onFriendAdded: () -> Unit,
    onOpenNfcReading: () -> Unit,
    pendingLinkedFriend: String? = null,
    onAddFriendEvent: (AddFriendViewModel.Event) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        pendingLinkedFriend?.let { username ->
            AddFriendDialog(
                username = username,
                trigger = onAddFriendEvent
            )
        }
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
                .safeDrawingPadding()
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
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(AppSize.icon), strokeWidth = 2.dp)
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                val actionsMaxHeight = maxHeight * 0.45f
                Column(
                    modifier = Modifier.widthIn(max = AppSize.contentMaxWidth).fillMaxSize().padding(horizontal = AppSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ScreenHeader(
                        title = stringResource(R.string.friends),
                        onBack = onBack,
                        modifier = Modifier.padding(vertical = AppSpacing.xs)
                    )
                    FriendsActions(
                        modifier = Modifier.heightIn(max = actionsMaxHeight).verticalScroll(rememberScrollState()),
                        onFriendAdded = onFriendAdded,
                        onOpenNfcReading = onOpenNfcReading,
                        onFriendsEvent = onFriendsEvent
                    )
                    FriendsListContent(
                        viewState = friendsViewState,
                        onEvent = onFriendsEvent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendsActions(
    modifier: Modifier = Modifier,
    onFriendAdded: () -> Unit,
    onOpenNfcReading: () -> Unit,
    onFriendsEvent: (FriendsViewModel.Event) -> Unit
) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val titles = listOf(stringResource(R.string.add_friend), stringResource(R.string.share_profile))
    Card(
        modifier = modifier.fillMaxWidth().padding(bottom = AppSpacing.md),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
            if (page == 0) {
                if (LocalInspectionMode.current) {
                    AddFriendContent(AddFriendViewModel.ViewState(addFriendNick = "", actionState = AsyncState.Idle), onEvent = {}, onOpenNfcReading = onOpenNfcReading)
                } else {
                    AddFriendContent(onFriendAdded = onFriendAdded, onOpenNfcReading = onOpenNfcReading)
                }
            } else {
                val onShowQr = { onFriendsEvent(FriendsViewModel.Event.OpenShareDialog) }
                val onTriggerNfc = { onFriendsEvent(FriendsViewModel.Event.OpenNfcSharingDialog) }
                if (LocalInspectionMode.current) {
                    ShareProfileContent(ShareProfileViewModel.ViewState(username = "Alex"), onEvent = {}, onShowQr = onShowQr, onTriggerNfc = onTriggerNfc)
                } else {
                    ShareProfileContent(onShowQr = onShowQr, onTriggerNfc = onTriggerNfc)
                }
            }
        }
    }
}

private fun processNdefMessage(message: NdefMessage?, context: Context, viewModel: FriendsViewModel) {
    var parsedUri: android.net.Uri? = null
    message?.records?.forEachIndexed { index, record ->
        Timber.tag(TAG).d("NFC record #%d: TNF=%d, payload length=%d", index, record.tnf, record.payload.size)
        if (parsedUri == null) {
            try {
                parsedUri = record.toUri()
                if (parsedUri != null) Timber.tag(TAG).d("Record #%d parsed as URI", index)
            } catch (e: Exception) {
                Timber.tag(TAG).d(e, "NFC record is not a URI")
            }
        }
    }

    val uri = parsedUri
    Timber.tag(TAG).d("NFC URI parsed: %s", uri != null)

    if (uri.isAddFriendDeepLink()) {
        (context as Activity).runOnUiThread {
            Timber.tag(TAG).d("Valid Friend URI received via NFC!")
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

private fun triggerNfcSharing(context: Context): Boolean {
    val activity = context as? Activity ?: return false
    val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return false
    if (keyguard.isDeviceLocked || keyguard.isKeyguardLocked) return false
    return try {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
        if (!adapter.isEnabled) return false
        val started = CardEmulation.getInstance(adapter).setPreferredService(
            activity, ComponentName(context, NdefHceService::class.java)
        )
        if (started) Toast.makeText(context, R.string.ready_to_share, Toast.LENGTH_SHORT).show()
        started
    } catch (exception: Exception) {
        Timber.tag(TAG).e(exception, "Unable to prefer NFC sharing service")
        false
    }
}

private fun stopNfcSharing(context: Context) {
    // Session authorization is revoked separately, even if platform cleanup fails.
    try {
        val activity = context as? Activity ?: return
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return
        CardEmulation.getInstance(adapter).unsetPreferredService(activity)
    } catch (exception: Exception) {
        Timber.tag(TAG).w(exception, "Unable to clear NFC service preference")
    }
}

private fun triggerNfcReading(context: Context, viewModel: FriendsViewModel) {
    Timber.tag(TAG).d("Triggering NFC Reading")
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    if (nfcAdapter == null) {
        Timber.tag(TAG).w("NFC is unavailable")
    } else {
        nfcAdapter.enableReaderMode(
            context as Activity,
            { tag ->
                try {
                    var message: NdefMessage? = null
                    val ndef = android.nfc.tech.Ndef.get(tag)
                    if (ndef != null) {
                        try {
                            ndef.connect()
                            message = ndef.ndefMessage
                        } catch (e: Exception) {
                            Timber.tag(TAG).d(e, "NDEF read failed; attempting fallback")
                        } finally {
                            try {
                                ndef.close()
                            } catch (e: Exception) {
                                Timber.tag(TAG).w(e, "Error closing Ndef")
                            }
                        }
                    }

                    if (message == null || message.records.isEmpty()) {
                        Timber.tag(TAG).d("NDEF read returned empty. Attempting manual fallback...")
                        val isoDep = android.nfc.tech.IsoDep.get(tag)
                        if (isoDep != null) {
                            try {
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
                            } catch (e: Exception) {
                                Timber.tag(TAG).w(e, "Error reading with IsoDep fallback")
                            } finally {
                                try {
                                    isoDep.close()
                                } catch (e: Exception) {
                                    Timber.tag(TAG).w(e, "Error closing IsoDep")
                                }
                            }
                        }
                    } else {
                        processNdefMessage(message, context, viewModel)
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Error reading NDEF tag")
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
