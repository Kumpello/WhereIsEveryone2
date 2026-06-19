package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun ControlBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .safeDrawingPadding()
            .padding(8.dp)
            .zIndex(1000f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun ControlButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    FloatingActionButton(onClick = onClick) {
        Icon(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            imageVector = imageVector,
            contentDescription = contentDescription
        )
    }
}
