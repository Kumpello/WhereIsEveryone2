package com.kumpello.whereiseveryone.common.ui.entities

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Button {

    @Composable
    fun Animated(
        modifier: Modifier = Modifier,
        text: String,
        fontSize: Int = 25,
        height: Int = 40,
        width: Int = 150,
        pressure: Float = 0.85f,
        onClick: () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        Box(
            modifier = modifier
                .height(height.dp)
                .width(width.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                modifier = Modifier
                    .fillMaxSize(
                        when {
                            isPressed -> pressure
                            else -> 1f
                        }
                    )
                    .animateContentSize(),
                onClick = onClick,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(
                    fontSize = when {
                        isPressed -> fontSize.sp * pressure
                        else -> fontSize.sp
                    },
                    text = text
                )
            }
        }
    }

}

@Preview(heightDp = 50)
@Composable
fun AnimatedPreview() {
    Button.Animated(
        text = "Login"
    ) {}
}