package com.kumpello.whereiseveryone.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
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

@Preview(name = "Light", showBackground = true, widthDp = 260, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 260, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AutoSizeHeightPreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Text.AutoSizeHeight(
                modifier = Modifier.padding(AppSpacing.md).width(220.dp).height(64.dp),
                text = "A longer status message that wraps to fit the available height."
            )
        }
    }
}
