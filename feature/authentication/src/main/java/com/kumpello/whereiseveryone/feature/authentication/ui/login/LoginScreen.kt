package com.kumpello.whereiseveryone.feature.authentication.ui.login

import com.kumpello.whereiseveryone.feature.authentication.R
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.feature.authentication.navigation.AuthenticationRoute
import com.kumpello.whereiseveryone.feature.authentication.ui.components.AuthLayout
import com.kumpello.whereiseveryone.feature.authentication.ui.components.RememberPasswordToggle
import com.kumpello.whereiseveryone.core.ui.components.TextField
import com.kumpello.whereiseveryone.core.presentation.ScreenState
import com.kumpello.whereiseveryone.core.presentation.AsyncState
import com.kumpello.whereiseveryone.core.ui.components.Button
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme

@Composable
fun LoginScreen(
    navController: NavController,
    onAuthenticated: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
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
                LoginViewModel.Action.CredentialsError -> Toast.makeText(
                    context, R.string.remember_password_error, Toast.LENGTH_LONG
                ).show()

                is LoginViewModel.Action.MakeToast -> Toast.makeText(context, action.string, Toast.LENGTH_SHORT)
                    .show()

                LoginViewModel.Action.NavigateMain -> {
                    onAuthenticated()
                }
                LoginViewModel.Action.NavigateSignUp -> navController.navigate(AuthenticationRoute.SignUp)
            }
        }
    }

    LoginScreen(
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
fun LoginScreen(
    viewState: LoginViewModel.ViewState,
    trigger: (LoginViewModel.Event) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val enabled = viewState.credentialsReady && !viewState.loginState.isLoading
    AuthLayout(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.login_subtitle)
    ) {
        TextField.Regular(
            label = stringResource(R.string.username_label),
            value = viewState.username,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            onValueChange = { trigger(LoginViewModel.Event.SetUsername(it)) }
        )
        TextField.Password(
            label = stringResource(R.string.password_label),
            value = viewState.password,
            enabled = enabled,
            onValueChange = { trigger(LoginViewModel.Event.SetPassword(it)) },
            passwordVisible = viewState.passwordVisible,
            onTogglePasswordVisibility = { trigger(LoginViewModel.Event.TogglePasswordVisibility) },
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (enabled) trigger(LoginViewModel.Event.OnLoginClick)
            })
        )
        RememberPasswordToggle(
            checked = viewState.rememberPassword,
            enabled = enabled,
            onToggle = { trigger(LoginViewModel.Event.ToggleRememberPassword) }
        )
        Button.Primary(
            text = stringResource(R.string.login_title),
            enabled = enabled,
            loading = viewState.loginState.isLoading
        ) {
            focusManager.clearFocus()
            trigger(LoginViewModel.Event.OnLoginClick)
        }
        Button.Secondary(text = stringResource(R.string.signup_here), enabled = enabled) {
            trigger(LoginViewModel.Event.NavigateSignUp)
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Large text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 2f)
@Preview(name = "Landscape", showBackground = true, widthDp = 740, heightDp = 360)
@Composable
fun LoginPreview() {
    WhereIsEveryoneTheme(false) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LoginScreen(
                LoginViewModel.ViewState(
                    screenState = ScreenState.Map,
                    username = "Janusz",
                    password = "dupadupadupa",
                    passwordVisible = false,
                    loginState = AsyncState.Idle
                )
            ) {}
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreviewDark() {
    WhereIsEveryoneTheme(true) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LoginScreen(
                LoginViewModel.ViewState(
                    screenState = ScreenState.Map,
                    username = "Janusz",
                    password = "dupadupadupa",
                    passwordVisible = false,
                    loginState = AsyncState.Idle
                )
            ) {}
        }
    }
}
