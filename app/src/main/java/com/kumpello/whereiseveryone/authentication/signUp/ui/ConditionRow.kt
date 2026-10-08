package com.kumpello.whereiseveryone.authentication.signUp.ui

import androidx.compose.runtime.Composable

@Composable
fun ConditionRow(condition: String, checked: Boolean) {
    ConditionItem(checked = checked, condition = condition)
}
