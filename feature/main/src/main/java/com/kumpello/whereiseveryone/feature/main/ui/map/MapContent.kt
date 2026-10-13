package com.kumpello.whereiseveryone.feature.main.ui.map

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.feature.main.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.feature.main.ui.components.FriendDetailsCard
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.kumpello.whereiseveryone.feature.main.location.LocationService

@Composable
fun MapContent(
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.map_placeholder))
        }
        return
    }

    val viewModel: MapViewModel = koinViewModel()
    val locationService: LocationService = koinInject()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.trigger(MapViewModel.Event.OnMapVisibilityChanged(true))
        onStopOrDispose {
            locationService.setMapDisplayLocation(null)
            viewModel.trigger(MapViewModel.Event.OnMapVisibilityChanged(false))
        }
    }
    LaunchedEffect(state.isCarMode, state.mapDisplayLocation) {
        locationService.setMapDisplayLocation(state.mapDisplayLocation.takeIf { state.isCarMode })
    }
    val windowSize = LocalWindowInfo.current.containerSize
    val landscape = windowSize.width > windowSize.height
    val navigationMaxWidth = if (landscape) {
        with(LocalDensity.current) { windowSize.width.toDp() / 2 }
    } else {
        AppSize.formMaxWidth
    }

    Box(modifier = modifier.fillMaxSize()) {
        Map(
            modifier = Modifier.fillMaxSize(),
            state = state.mapSettings,
            actions = viewModel.action,
            friendsPositions = state.friends,
            event = viewModel::trigger,
            trackCameraBearing = state.navigatingFriend != null,
            isCarMode = state.isCarMode,
        )

        if (state.isCarMode) {
            DrivingSpeedCard(
                speedKmh = state.speedKmh,
                modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding()
                    .padding(end = AppSpacing.md, bottom = AppSpacing.xxl + AppSpacing.xl)
                    .width(AppSize.touchTarget * 3 + AppSpacing.md),
            )
        }

        if (state.showCarSuggestion) {
            CarSuggestionDialog(
                onConfirm = { viewModel.trigger(MapViewModel.Event.AcceptCarSuggestion) },
                onDismiss = { viewModel.trigger(MapViewModel.Event.DismissCarSuggestion) },
            )
        }

        if (state.selectedFriend != null) {
            FriendDetailsCard(
                friend = state.selectedFriend!!,
                onDismiss = { viewModel.trigger(MapViewModel.Event.DismissFriendDetails) },
                onNavigate = { friend ->
                    viewModel.trigger(
                        MapViewModel.Event.NavigateToFriend(
                            friend
                        )
                    )
                },
                onSharingToggle = { friend ->
                    viewModel.trigger(
                        MapViewModel.Event.ToggleSharing(
                            friend.username
                        )
                    )
                }
            )
        }

        if (state.navigatingFriend != null && state.bearingToFriend != null) {
            NavigationCompass(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .widthIn(max = navigationMaxWidth)
                    .padding(
                        top = if (landscape) AppSpacing.md else AppSpacing.xxl * 2,
                        start = AppSpacing.md,
                        end = AppSpacing.md
                    ),
                bearing = state.bearingToFriend!!,
                friendName = state.navigatingFriend!!.username,
                distance = state.navigatingFriend!!.formattedDistance,
                carTrackingInfo = state.carTrackingInfo,
                onCancel = { viewModel.trigger(MapViewModel.Event.CancelNavigation) }
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapContentPreview() {
    WhereIsEveryoneTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MapContent(modifier = Modifier.fillMaxSize())
        }
    }
}
