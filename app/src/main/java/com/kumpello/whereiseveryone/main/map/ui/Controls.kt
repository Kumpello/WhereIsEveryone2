package com.kumpello.whereiseveryone.main.map.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kumpello.whereiseveryone.common.ui.theme.AppElevation
import com.kumpello.whereiseveryone.common.ui.theme.AppSize
import com.kumpello.whereiseveryone.common.ui.theme.AppSpacing

@Composable
fun ControlBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = modifier.safeDrawingPadding().padding(AppSpacing.md).zIndex(1000f),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = AppElevation.floating
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlButton(imageVector: ImageVector, contentDescription: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(contentDescription) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(AppSize.touchTarget)) {
            Icon(imageVector, contentDescription = contentDescription, modifier = Modifier.size(AppSize.icon))
        }
    }
}
