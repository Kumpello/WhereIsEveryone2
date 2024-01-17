package com.kumpello.whereiseveryone.common.ui.entity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumpello.whereiseveryone.common.extension.bounceClick

object Button {

    @Composable
    fun Animated(
        modifier: Modifier = Modifier,
        text: String,
        textSize: Int = 16,
        height: Int = 40,
        width: Int = 150,
        onClick: () -> Unit
    ) {
        Box(
            modifier = modifier
                .height(height.dp)
                .width(width.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                modifier = Modifier
                    .fillMaxSize()
                    .bounceClick(),
                onClick = onClick,
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = textSize.sp
                    )
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