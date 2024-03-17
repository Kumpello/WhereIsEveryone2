package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme

@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    SmallFloatingActionButton(
        onClick = { onClick() },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.secondary
    ) {
        icon()
    }
}

@Preview(showBackground = true)
@Composable
fun FloatingActionButtonPreview() {
    WhereIsEveryoneTheme {
        FloatingActionButton(onClick = { }) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FloatingActionButtonPreviewDark() {
    WhereIsEveryoneTheme(true){
        FloatingActionButton(onClick = { }) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "")
        }
    }
}