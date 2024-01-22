package com.kumpello.whereiseveryone.authentication.signUp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConditionRow(
    condition: String,
    checked: Boolean
) {
    val color by animateColorAsState(
        targetValue = if (checked) Color.Green else Color.Red,
        label = "text color"
    )

    val icon = if (checked) {
        Icons.Rounded.Check
    } else {
        Icons.Rounded.Close
    }

    Row {
        Icon(
            imageVector = icon,
            contentDescription = "status icon",
            tint = color
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = condition,
            color = color
        )
    }
}