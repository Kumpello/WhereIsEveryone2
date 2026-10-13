package com.kumpello.whereiseveryone.feature.main.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kumpello.whereiseveryone.core.ui.theme.Shapes

@Composable
fun FloatingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = Shapes.large,
    ){
        content()
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FloatingCardPreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FloatingCard(modifier = Modifier.padding(AppSpacing.md)) {
                Column(
                    modifier = Modifier.padding(AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Text("Anna", style = MaterialTheme.typography.titleLarge)
                    Text("850 m away", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
