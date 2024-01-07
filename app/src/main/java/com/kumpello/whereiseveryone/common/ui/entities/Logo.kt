package com.kumpello.whereiseveryone.common.ui.entities

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

object Logo {

    @Composable
    fun Text(
        modifier: Modifier = Modifier,
        size: Int = 35
    ) {
        Text(
            modifier = modifier,
            text = "Where is Everyone!?",
            style = TextStyle(
                fontSize = size.sp,
                fontFamily = FontFamily.Serif
            )
        )
    }
}

@Preview
@Composable
fun TextPreview() {
    Logo.Text()
}