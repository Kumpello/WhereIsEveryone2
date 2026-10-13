package com.kumpello.whereiseveryone.feature.authentication.ui.signup

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import androidx.compose.runtime.Composable

@Composable
fun ConditionRow(condition: String, checked: Boolean) {
    ConditionItem(checked = checked, condition = condition)
}

@Preview(name = "Light", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConditionRowPreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                ConditionRow(condition = "At least 8 characters", checked = true)
                ConditionRow(condition = "An uppercase letter", checked = false)
            }
        }
    }
}
