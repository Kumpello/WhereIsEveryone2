package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.main.common.ui.FriendDetailsCard
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
                onNavigate = { friend -> viewModel.trigger(MapViewModel.Event.NavigateToFriend(friend)) },
                onSharingToggle = { friend -> viewModel.trigger(MapViewModel.Event.ToggleSharing(friend.username)) }
            )
        }

        if (state.navigatingFriend != null && state.bearingToFriend != null) {
            NavigationCompass(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .padding(8.dp),
                bearing = state.bearingToFriend!!,
                friendName = state.navigatingFriend!!.username,
                onCancel = { viewModel.trigger(MapViewModel.Event.CancelNavigation) }
            )
        }
    }
}

@Composable
private fun NavigationCompass(
    modifier: Modifier = Modifier,
    bearing: Float,
    friendName: String,
    onCancel: () -> Unit
) {
    val scale = remember { Animatable(1f) }

    Card(
        modifier = modifier
            .zIndex(1001f)
            .scale(scale.value)
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        val animationJob = this@coroutineScope.launch {
                            val result = scale.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                            )
                            if (result.endReason == AnimationEndReason.Finished) {
                                onCancel()
                            }
                        }
                        waitForUpOrCancellation()
                        animationJob.cancel()
                        this@coroutineScope.launch {
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 300)
                            )
                        }
                    }
                }
            },
        shape = com.kumpello.whereiseveryone.common.ui.theme.Shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(R.string.direction_to_format, friendName),
                modifier = Modifier
                    .size(32.dp)
                    .rotate(bearing),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = friendName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${bearing.toInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
