package com.kumpello.whereiseveryone.ui.main.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
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
    }

}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
    WhereIsEveryoneTheme {
        Map(rememberNavController(), MainActivityViewModel())
    }
}