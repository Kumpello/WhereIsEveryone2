package com.kumpello.whereiseveryone.authentication.common.ui.entity

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.style.TextOverflow

object Text {

    @Composable
    fun AutoSizeHeight(
        modifier: Modifier = Modifier
    ) {
        val textStyleMedium = MaterialTheme.typography.bodyMedium
        var textStyle by remember { mutableStateOf(textStyleMedium) }
        var readyToDraw by remember { mutableStateOf(false) }
        Text(
            text = "long text goes here",
            style = textStyle,
            overflow = TextOverflow.Clip,
            modifier = modifier.drawWithContent {
                if (readyToDraw) drawContent()
            },
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.didOverflowHeight) {
                    textStyle = textStyle.copy(fontSize = textStyle.fontSize * 0.9)
                } else {
                    readyToDraw = true
                }
            }
        )
    }
}