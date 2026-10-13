package com.kumpello.whereiseveryone.feature.authentication.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.feature.authentication.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.kumpello.whereiseveryone.core.ui.theme.AppSize
import com.kumpello.whereiseveryone.core.ui.theme.AppSpacing

@Composable
fun RememberPasswordToggle(checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = AppSize.touchTarget)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Checkbox(checked = checked, enabled = enabled, onCheckedChange = null)
        Text(
            text = stringResource(R.string.remember_password),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RememberPasswordTogglePreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                RememberPasswordToggle(checked = true, enabled = true, onToggle = {})
                RememberPasswordToggle(checked = false, enabled = true, onToggle = {})
                RememberPasswordToggle(checked = true, enabled = false, onToggle = {})
            }
        }
    }
}
