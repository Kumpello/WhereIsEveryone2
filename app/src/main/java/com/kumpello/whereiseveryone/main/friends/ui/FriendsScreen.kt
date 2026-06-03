package com.kumpello.whereiseveryone.main.friends.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.shortToast
import com.kumpello.whereiseveryone.common.ui.theme.Shapes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.Location
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
    val state by viewModel.viewState.collectAsStateWithLifecycle()
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
                viewModel.checkFriends()

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
    trigger: (FriendsViewModel.Command) -> Unit,
) {
    //TODO: Split by type
    if (viewState.deleteFriendDialogState is DeleteFriendDialogState.Open) {
        DeleteFriendDialog(
            friend = viewState.deleteFriendDialogState.friend,
            trigger = trigger
        )
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(
                vertical = 64.dp,
                horizontal = 8.dp
            ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = Shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextField( //TODO: Make it more funky, maybe other composable? To consider in all Textfields
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Your friends nick") },
                        value = viewState.addFriendNick,
                        onValueChange = { nick ->
                            trigger(FriendsViewModel.Command.SetAddFriendNick(nick))
                        })
                    Spacer(Modifier.size(20.dp))
                    Button.Animated(
                        text = stringResource(R.string.add_friend),
                        width = 250
                    ) {
                        trigger(FriendsViewModel.Command.AddFriend)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(viewState.friends) { friend -> //TODO: OnClick with popup for friend info
                    Friend(
                        friend = friend,
                        trigger = trigger
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPreview() { //TODO: Get this preview unfucked
    WhereIsEveryoneTheme(darkTheme = true) {
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
                            accuracy = AccuracyLevel.MEDIUM,
                            lastUpdateTime = "20.04.2137",
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
                            accuracy = AccuracyLevel.PERFECT,
                            lastUpdateTime = "20.04.2137",
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
                            accuracy = AccuracyLevel.TRAGIC,
                            lastUpdateTime = "20.04.2137",
                            lastUpdateAge = LastUpdateAge.OLD,
                        )
                    )
                ),
                addFriendNick = "Papator2000",
                deleteFriendDialogState = DeleteFriendDialogState.Closed
            )
        ) {}
    }
}