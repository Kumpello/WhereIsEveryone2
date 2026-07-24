package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.main.common.ui.FriendDetailsCard
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapContent(
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.map_placeholder))
        }
        return
    }

    val viewModel: MapViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Map(
            modifier = Modifier.fillMaxSize(),
            state = state.mapSettings,
            actions = viewModel.action,
            userLocation = state.user,
            friendsPositions = state.friends,
            event = viewModel::trigger
        )

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
                    .padding(
                        top = 64.dp,
                        start = 4.dp
                    ),
                bearing = state.bearingToFriend!!,
                friendName = state.navigatingFriend!!.username,
                distance = state.navigatingFriend!!.formattedDistance,
                onCancel = { viewModel.trigger(MapViewModel.Event.CancelNavigation) }
            )
        }
    }
}
