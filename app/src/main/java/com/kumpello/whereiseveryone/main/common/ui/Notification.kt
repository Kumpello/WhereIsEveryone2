package com.kumpello.whereiseveryone.main.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel

@Composable
fun Notification(
    modifier: Modifier = Modifier,
    notification: String,
    onAllowClick: () -> Unit,
    onDenyClick: () -> Unit
) {
    FloatingCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = notification)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button.Animated(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.deny)
                ) {
                    onDenyClick()
                }
                Button.Animated(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.allow)
                ) {
                    onAllowClick()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationPreview() {
    Notification(
        modifier = Modifier
            .zIndex(Float.MAX_VALUE)
            .padding(8.dp),
        notification = stringResource(R.string.permissions_message),
        onAllowClick = { },
        onDenyClick = { }
    )
}