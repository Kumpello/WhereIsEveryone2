package com.kumpello.whereiseveryone.feature.main.ui.map

import com.kumpello.whereiseveryone.feature.main.R
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.core.presentation.ScreenState
import com.kumpello.whereiseveryone.core.ui.components.AppDialog
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.feature.main.navigation.MainRoute
import com.kumpello.whereiseveryone.feature.main.ui.components.Notification
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapScreen(
    navController: NavController,
    screenViewModel: MapScreenViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val screenState by screenViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        screenViewModel.action.collect { action ->
            when (action) {
                MapScreenViewModel.Action.NavigateFriends -> navController.navigate(MainRoute.Friends)
                MapScreenViewModel.Action.NavigateSettings -> navController.navigate(MainRoute.Settings)
                is MapScreenViewModel.Action.Toast -> Toast.makeText(
                    context,
                    action.id,
                    Toast.LENGTH_SHORT
                ).show()

                is MapScreenViewModel.Action.ShowPermissionSettings -> { /* Handle in Activity */
                }
            }
        }
    }

    BackHandler(screenState.screenState != ScreenState.Map) {
        screenViewModel.trigger(MapScreenViewModel.Event.BackToMap)
    }

    MapScreenContent(
        screenViewState = screenState,
        onScreenEvent = screenViewModel::trigger
    )
}

@Composable
private fun MapScreenContent(
    screenViewState: MapScreenViewModel.ViewState,
    onScreenEvent: (MapScreenViewModel.Event) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MapContent(Modifier.fillMaxSize())

        MapTopControls(
            modifier = Modifier.align(Alignment.TopEnd),
            onEvent = onScreenEvent
        )

        if (screenViewState.screenState is ScreenState.Message) {
            AppDialog(onDismiss = { onScreenEvent(MapScreenViewModel.Event.BackToMap) }) {
                MessageFloatingCard(
                    onMessageSent = { onScreenEvent(MapScreenViewModel.Event.BackToMap) },
                    onClose = { onScreenEvent(MapScreenViewModel.Event.BackToMap) }
                )
            }
        }

        if (screenViewState.showPermissionNotification) {
            val baseMessage = stringResource(R.string.permissions_message)
            val warningMessage = stringResource(R.string.permissions_allow_all_the_time_warning)
            val errorColor = androidx.compose.material3.MaterialTheme.colorScheme.error
            val annotatedMessage = buildAnnotatedString {
                append(baseMessage)
                append("\n\n")
                withStyle(
                    style = SpanStyle(
                        color = errorColor,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(warningMessage)
                }
            }
            Notification(
                notification = annotatedMessage,
                onAllowClick = { onScreenEvent(MapScreenViewModel.Event.OnPermissionAllow) },
                onDenyClick = { onScreenEvent(MapScreenViewModel.Event.OnPermissionDeny) },
                onDismiss = { onScreenEvent(MapScreenViewModel.Event.OnPermissionDeny) }
            )
        }

        if (screenViewState.showFindMeDialog) {
            FindMeDialog(
                isForcedEnabled = screenViewState.isForcedForegroundEnabled,
                endTime = screenViewState.forcedForegroundEndTime,
                onDismiss = { onScreenEvent(MapScreenViewModel.Event.DismissFindMeDialog) },
                onEnable = { seconds -> onScreenEvent(MapScreenViewModel.Event.EnableForcedForeground(seconds)) },
                onDisable = { onScreenEvent(MapScreenViewModel.Event.DisableForcedForeground) }
            )
        }

        MapBottomControls(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    WhereIsEveryoneTheme {
        MapScreenContent(
            screenViewState = MapScreenViewModel.ViewState(
                showPermissionNotification = true,
                permissions = emptyMap(),
                screenState = ScreenState.Map,
                showFindMeDialog = false,
                isForcedForegroundEnabled = false,
                forcedForegroundEndTime = null
            ),
            onScreenEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenMessagePreview() {
    WhereIsEveryoneTheme {
        MapScreenContent(
            screenViewState = MapScreenViewModel.ViewState(
                showPermissionNotification = false,
                permissions = emptyMap(),
                screenState = ScreenState.Message,
                showFindMeDialog = false,
                isForcedForegroundEnabled = false,
                forcedForegroundEndTime = null
            ),
            onScreenEvent = {}
        )
    }
}
