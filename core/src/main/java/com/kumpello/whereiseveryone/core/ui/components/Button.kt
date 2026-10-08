package com.kumpello.whereiseveryone.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing

object Button {
    @Composable
    fun Primary(
        text: String,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        loading: Boolean = false,
        onClick: () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "Button press")
        MaterialButton(
            onClick = onClick,
            enabled = enabled && !loading,
            interactionSource = interactionSource,
            modifier = modifier.fillMaxWidth().heightIn(min = AppSize.button)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            shape = MaterialTheme.shapes.medium,
            colors = if (loading) ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            ) else ButtonDefaults.buttonColors()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp
                    )
                }
                Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }

    @Composable
    fun Secondary(
        text: String,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        destructive: Boolean = false,
        icon: ImageVector? = null,
        onClick: () -> Unit
    ) {
        val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.fillMaxWidth().heightIn(min = AppSize.button),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, if (enabled) color else MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(AppSize.icon)) }
                Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }

    // Keep the original entry points for existing callers; new screens use the semantic variants.
    @Composable
    fun Animated(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        text: String,
        textSize: Int = 16,
        height: Int = 52,
        width: Int = 150,
        onClick: () -> Unit
    ) {
        LegacyButton(modifier.width(width.dp).heightIn(min = height.dp), enabled, text, textSize, onClick)
    }

    @Composable
    fun Animated(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        text: String,
        textSize: Int = 16,
        onClick: () -> Unit
    ) {
        LegacyButton(modifier, enabled, text, textSize, onClick)
    }
}

@Composable
private fun LegacyButton(modifier: Modifier, enabled: Boolean, text: String, textSize: Int, onClick: () -> Unit) {
    MaterialButton(
        modifier = modifier.fillMaxWidth().heightIn(min = AppSize.button),
        enabled = enabled,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = textSize.sp), textAlign = TextAlign.Center)
    }
}
