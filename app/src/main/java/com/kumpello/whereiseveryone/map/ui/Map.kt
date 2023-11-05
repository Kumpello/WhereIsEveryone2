package com.kumpello.whereiseveryone.map.ui

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.map.data.model.UserPosition
import com.kumpello.whereiseveryone.map.domain.events.GetPositionsEvent
import com.kumpello.whereiseveryone.map.presentation.MainActivityViewModel
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@Composable
fun Map(navController: NavHostController, viewModel: MainActivityViewModel) {
    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val application = mContext.applicationContext as WhereIsEveryoneApplication
    val state = viewModel.uiState.value
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(state.position, state.zoom)
    }

    var friends: List<UserPosition> = listOf()

    LaunchedEffect(mContext) {
        viewModel.event.collect { event ->
            when (event) {
                is GetPositionsEvent.GetError -> Toast
                    .makeText(mContext, event.error.message, Toast.LENGTH_SHORT)
                    .show()

                is GetPositionsEvent.GetSuccess -> friends = event.organizationsData.positions
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        properties = state.properties,
        uiSettings = state.mapUiSettings,
        cameraPositionState = cameraPositionState
    ) {
        Switch(
            checked = state.controlState,
            onCheckedChange = {
                viewModel.onEvent(UIEvent.SwitchControl(it))
            }
        )
        IconButton(
            onClick = {
                navController.navigate(MainRoutes.Friends.route)
            },
            modifier = Modifier
                .height(50.dp)
        ) {
            Icon(
                modifier = Modifier.size(size = 50.dp),
                imageVector = Icons.Outlined.List,
                contentDescription = "",
                tint = androidx.compose.ui.graphics.Color.Red
            )
        }
        Marker(
            state = MarkerState(position = state.position),
            title = stringResource(R.string.user_marker)
            //,snippet = "Marker in Singapore"
        )
        friends.forEach {
            Marker(
                state = MarkerState(position = it.location),
                title = it.nick
            )
        }
    }

}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
    WhereIsEveryoneTheme {
        Map(rememberNavController(), MainActivityViewModel())
    }
}