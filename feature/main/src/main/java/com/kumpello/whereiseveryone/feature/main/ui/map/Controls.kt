package com.kumpello.whereiseveryone.feature.main.ui.map

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kumpello.whereiseveryone.core.ui.theme.AppElevation
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing

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
fun ControlButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(contentDescription) } },
        state = rememberTooltipState()
    ) {
        Box(modifier = modifier.widthIn(min = AppSize.touchTarget).height(AppSize.touchTarget)) {
            IconButton(onClick = onClick, modifier = Modifier.matchParentSize()) {
                Icon(imageVector, contentDescription = contentDescription, modifier = Modifier.size(AppSize.icon))
            }
            if (badgeCount > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .padding(top = AppSpacing.xxs, end = AppSpacing.xxs)
                        .clearAndSetSemantics {}
                ) { Text(badgeCount.toString()) }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ControlBarPreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ControlBar {
                ControlButton(imageVector = Icons.Default.Person, contentDescription = "Friends", onClick = {})
                ControlButton(imageVector = Icons.Default.Settings, contentDescription = "Settings", onClick = {})
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ControlButtonPreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ControlButton(
                modifier = Modifier.padding(AppSpacing.md),
                imageVector = Icons.Default.Person,
                contentDescription = "Friends",
                onClick = {}
            )
        }
    }
}

@Preview(name = "Pending requests", showBackground = true)
@Preview(name = "Pending requests, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Pending requests, large text", showBackground = true, fontScale = 2f)
@Composable
private fun ControlButtonBadgePreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ControlBar {
                ControlButton(imageVector = Icons.Default.Person, contentDescription = "Friends, 3 requests awaiting acceptance", badgeCount = 3, onClick = {})
                ControlButton(imageVector = Icons.Default.Person, contentDescription = "Friends, 2048 requests awaiting acceptance", badgeCount = 2048, onClick = {})
            }
        }
    }
}
