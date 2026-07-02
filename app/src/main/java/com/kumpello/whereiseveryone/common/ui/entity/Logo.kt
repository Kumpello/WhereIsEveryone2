package com.kumpello.whereiseveryone.common.ui.entity

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.kumpello.whereiseveryone.R

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

    @Composable
    fun Image(
        modifier: Modifier = Modifier,
    ) {
        androidx.compose.foundation.Image(
            modifier = modifier,
            contentScale = ContentScale.FillWidth,
            painter = painterResource(id = R.drawable.im_app_name),
            contentDescription = "App name",
        )
    }
}

@Preview
@Composable
fun TextPreview() {
    Logo.Text()
}