package com.kumpello.whereiseveryone.authentication.common.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.kumpello.whereiseveryone.R

@Composable
fun RememberPasswordToggle(checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconToggleButton(checked = checked, enabled = enabled, onCheckedChange = { onToggle() }) {
            Icon(
                imageVector = if (checked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = stringResource(R.string.remember_password)
            )
        }
        Text(text = stringResource(R.string.remember_password), style = MaterialTheme.typography.bodyMedium)
    }
}
