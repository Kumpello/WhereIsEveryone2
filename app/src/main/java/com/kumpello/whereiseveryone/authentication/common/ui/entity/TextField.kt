package com.kumpello.whereiseveryone.authentication.common.ui.entity

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

object TextField {

    @Composable
    fun Regular(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
    ) {
        TextField(
            label = { Text(text = label) },
            value = value,
            onValueChange = { username ->
                onValueChange(username)
            })
    }

    @Composable
    fun Password(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
    ) {
        TextField(
            label = { Text(text = label) },
            value = value,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = { password ->
                onValueChange(password)
            })
    }
}