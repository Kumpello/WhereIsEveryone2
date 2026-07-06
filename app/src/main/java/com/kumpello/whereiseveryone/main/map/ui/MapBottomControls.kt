package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapBottomControls(
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        MapBottomControls(
            modifier = modifier,
            onEvent = {}
        )
        return
    }

    val viewModel: MapViewModel = koinViewModel()
    MapBottomControls(
        modifier = modifier,
        onEvent = viewModel::trigger
    )
}

@Composable
fun MapBottomControls(
    modifier: Modifier = Modifier,
    onEvent: (MapViewModel.Event) -> Unit
) {
    ControlBar(modifier = modifier) {
        ControlButton(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Zoom out",
            onClick = { onEvent(MapViewModel.Event.ZoomOut) }
        )
        ControlButton(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Zoom in",
            onClick = { onEvent(MapViewModel.Event.ZoomIn) }
        )
        ControlButton(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Center",
            onClick = { onEvent(MapViewModel.Event.CenterMap) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapBottomControlsPreview() {
    WhereIsEveryoneTheme {
        MapBottomControls(onEvent = {})
    }
}
