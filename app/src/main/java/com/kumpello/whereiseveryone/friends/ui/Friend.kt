package com.kumpello.whereiseveryone.friends.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun Friend(
    navigator: DestinationsNavigator,
    nick: String
) {
    var dialogOpen by remember {
        mutableStateOf(false)
    }
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back button.
                // If you want to disable that functionality, simply leave this block empty.
                dialogOpen = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // perform the confirm action and
                        // close the dialog
                        dialogOpen = false
                    }
                ) {
                    Text(text = "Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // close the dialog
                        dialogOpen = false
                    }
                ) {
                    Text(text = "Dismiss")
                }
            },
            title = {
                Text(text = "Confirmation")
            },
            text = {
                Text(text = "Are you sure you want to delete $nick from your friends?")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(5.dp)
        )
    }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(nick)
            IconButton(
                onClick = {
                    dialogOpen = true
                },
                modifier = Modifier
                    .height(50.dp)
            ) {
                Icon(
                    modifier = Modifier.size(size = 50.dp),
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "",
                    tint = Color.Red
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Friend() {
    WhereIsEveryoneTheme() {
        Friend("JanuszAndrzejNowak")
    }
}