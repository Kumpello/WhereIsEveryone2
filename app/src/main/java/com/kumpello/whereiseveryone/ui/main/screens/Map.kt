package com.kumpello.whereiseveryone.ui.main.screens

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.data.model.map.UserPosition
import com.kumpello.whereiseveryone.domain.events.GetPositionsEvent
import com.kumpello.whereiseveryone.domain.events.UIEvent
import com.kumpello.whereiseveryone.ui.main.MainActivityViewModel
import com.kumpello.whereiseveryone.ui.theme.WhereIsEveryoneTheme

@Composable
fun Map(navController: NavHostController, viewModel: MainActivityViewModel) {
    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val application = mContext.applicationContext as WhereIsEveryoneApplication
    val state = viewModel.uiState.value
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(state.position, state.zoom)
    }

    lateinit var friends: List<UserPosition>

    LaunchedEffect(mContext) {
        viewModel.event.collect { event ->
            when (event) {
                is GetPositionsEvent.GetError -> Toast
                    .makeText(mContext, event.error.errorBody.string(), Toast.LENGTH_SHORT)
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