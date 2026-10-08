package com.kumpello.whereiseveryone.feature.authentication.ui.signup

import androidx.compose.runtime.Composable

@Composable
fun ConditionRow(condition: String, checked: Boolean) {
    ConditionItem(checked = checked, condition = condition)
}
