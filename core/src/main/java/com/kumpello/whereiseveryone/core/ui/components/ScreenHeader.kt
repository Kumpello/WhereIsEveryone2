package com.kumpello.whereiseveryone.core.ui.components

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.core.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing

@Composable
fun ScreenHeader(modifier: Modifier = Modifier, title: String, onBack: () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(AppSize.touchTarget)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_map))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 88, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 88, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScreenHeaderPreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ScreenHeader(title = "Friends", onBack = {})
        }
    }
}
