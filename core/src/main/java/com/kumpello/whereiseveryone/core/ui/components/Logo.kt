package com.kumpello.whereiseveryone.core.ui.components

import com.kumpello.whereiseveryone.core.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
            text = stringResource(R.string.app_name_full),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = size.sp)
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
            contentDescription = stringResource(R.string.app_name),
            colorFilter = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            } else null
        )
    }
}

@Preview
@Composable
fun TextPreview() {
    Logo.Text()
}
