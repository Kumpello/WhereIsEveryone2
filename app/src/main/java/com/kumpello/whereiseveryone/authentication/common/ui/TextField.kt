package com.kumpello.whereiseveryone.authentication.common.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.kumpello.whereiseveryone.R

object TextField {
    @Composable
    fun Regular(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        labelColor: Color = Color.Unspecified,
        colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
        trailingIcon: @Composable (() -> Unit)? = null,
        enabled: Boolean = true,
        singleLine: Boolean = true,
        keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions: KeyboardActions = KeyboardActions.Default
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            label = { Text(label, color = labelColor) },
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            trailingIcon = trailingIcon
        )
    }

    @Composable
    fun Password(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        passwordVisible: Boolean,
        onTogglePasswordVisibility: () -> Unit,
        enabled: Boolean = true,
        keyboardActions: KeyboardActions = KeyboardActions.Default
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            enabled = enabled,
            label = { Text(label) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = keyboardActions,
            onValueChange = onValueChange,
            shape = MaterialTheme.shapes.medium,
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility, enabled = enabled) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.hide_password_cd else R.string.show_password_cd
                        )
                    )
                }
            }
        )
    }
}
