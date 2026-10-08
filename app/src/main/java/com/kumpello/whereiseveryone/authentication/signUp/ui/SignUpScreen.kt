package com.kumpello.whereiseveryone.authentication.signUp.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.AuthenticationActivity
import com.kumpello.whereiseveryone.authentication.common.AuthenticationRoute
import com.kumpello.whereiseveryone.authentication.common.ui.AuthLayout
import com.kumpello.whereiseveryone.authentication.common.ui.RememberPasswordToggle
import com.kumpello.whereiseveryone.authentication.common.ui.TextField
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.PasswordValidationState
import com.kumpello.whereiseveryone.authentication.signUp.presentation.SignUpViewModel
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.theme.AppSpacing
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainActivity

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: SignUpViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? AuthenticationActivity
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val keyboardVisible =
        WindowInsets.ime.getBottom(LocalDensity.current) > 0

    BackHandler(enabled = keyboardVisible) {
        focusManager.clearFocus()
    }

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                SignUpViewModel.Action.CredentialsError -> Toast.makeText(
                    context, R.string.remember_password_error, Toast.LENGTH_LONG
                ).show()

                is SignUpViewModel.Action.MakeToast -> Toast.makeText(context, action.string, Toast.LENGTH_SHORT)
                    .show()

                SignUpViewModel.Action.NavigateMain -> {
                    context.startActivity(Intent(context, MainActivity::class.java))
                    activity?.finish()
                }
                SignUpViewModel.Action.NavigateLogin -> navController.navigate(AuthenticationRoute.Login)
            }
        }
    }

    SignUpScreen(
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
fun SignUpScreen(
    viewState: SignUpViewModel.ViewState,
    trigger: (SignUpViewModel.Event) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val enabled = viewState.credentialsReady && !viewState.signUpState.isLoading
    val canSubmit = enabled && viewState.passwordState.successful
    AuthLayout(
        title = stringResource(R.string.signup_title),
        subtitle = stringResource(R.string.signup_subtitle)
    ) {
        TextField.Regular(
            label = stringResource(R.string.username_label),
            value = viewState.username,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            onValueChange = { trigger(SignUpViewModel.Event.SetUsername(it)) }
        )
        TextField.Password(
            label = stringResource(R.string.password_label),
            value = viewState.password,
            enabled = enabled,
            onValueChange = { trigger(SignUpViewModel.Event.SetPassword(it)) },
            passwordVisible = viewState.passwordVisible,
            onTogglePasswordVisibility = { trigger(SignUpViewModel.Event.TogglePasswordVisibility) },
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (canSubmit) trigger(SignUpViewModel.Event.OnSignUpClick)
            })
        )
        RememberPasswordToggle(
            checked = viewState.rememberPassword,
            enabled = enabled,
            onToggle = { trigger(SignUpViewModel.Event.ToggleRememberPassword) }
        )
        Conditions(viewState)
        Button.Primary(
            text = stringResource(R.string.signup_title),
            enabled = canSubmit,
            loading = viewState.signUpState.isLoading
        ) {
            focusManager.clearFocus()
            trigger(SignUpViewModel.Event.OnSignUpClick)
        }
        Button.Secondary(text = stringResource(R.string.login_here), enabled = enabled) {
            trigger(SignUpViewModel.Event.NavigateLogin)
        }
    }
}

@Composable
fun Conditions(viewState: SignUpViewModel.ViewState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.password_requirements),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = AppSpacing.xxs)
        )
        ConditionItem(
            checked = viewState.passwordState.hasMinimum,
            condition = stringResource(R.string.minimum_8_characters)
        )
        ConditionItem(
            checked = viewState.passwordState.hasSpecialCharacter,
            condition = stringResource(R.string.minimum_1_special_character)
        )
        ConditionItem(
            checked = viewState.passwordState.hasCapitalizedLetter,
            condition = stringResource(R.string.minimum_1_capitalized_letter)
        )
        ConditionItem(
            checked = viewState.passwordState.noWhitespaces,
            condition = stringResource(R.string.no_whitespaces)
        )
    }
}

@Composable
fun ConditionItem(checked: Boolean, condition: String) {
    val description = stringResource(if (checked) R.string.requirement_met else R.string.requirement_not_met)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = if (checked) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = condition,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Large text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 2f)
@Preview(name = "Landscape", showBackground = true, widthDp = 740, heightDp = 360)
@Composable
fun SignUpPreview() {
    WhereIsEveryoneTheme(false) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SignUpScreen(
                SignUpViewModel.ViewState(
                    screenState = ScreenState.Map,
                    username = "Janusz",
                    password = "dupadupadupa",
                    passwordVisible = false,
                    passwordState = PasswordValidationState(
                        hasMinimum = true,
                        hasSpecialCharacter = true,
                        hasCapitalizedLetter = true,
                        noWhitespaces = true,
                        successful = true
                    ),
                    signUpState = AsyncState.Idle
                )
            ) {}
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpPreviewDark() {
    WhereIsEveryoneTheme(true) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SignUpScreen(
                SignUpViewModel.ViewState(
                    screenState = ScreenState.Map,
                    username = "Janusz",
                    password = "dupadupadupa",
                    passwordVisible = false,
                    passwordState = PasswordValidationState(
                        hasMinimum = true,
                        hasSpecialCharacter = true,
                        hasCapitalizedLetter = true,
                        noWhitespaces = true,
                        successful = true
                    ),
                    signUpState = AsyncState.Idle
                )
            ) {}
        }
    }
}
