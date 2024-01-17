package com.kumpello.whereiseveryone.common.ui.entity

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

object Text {

    @Composable
    fun AutoSizeHeight(
        modifier: Modifier = Modifier,
        text: String,
        textStyle: TextStyle = MaterialTheme.typography.titleLarge
    ) {
        var resizedTextStyle by remember { mutableStateOf(textStyle) }
        var readyToDraw by remember { mutableStateOf(false) }
        Text(
            text = text,
            style = resizedTextStyle,
            overflow = TextOverflow.Clip,
            modifier = modifier.drawWithContent {
                if (readyToDraw) drawContent()
            },
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.didOverflowHeight) {
                    resizedTextStyle = resizedTextStyle.copy(fontSize = resizedTextStyle.fontSize * 0.95)
                } else {
                    readyToDraw = true
                }
            }
        )
    }
}