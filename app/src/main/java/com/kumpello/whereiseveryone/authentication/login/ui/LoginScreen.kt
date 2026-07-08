package com.kumpello.whereiseveryone.authentication.login.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.authentication.common.AuthenticationRoute
import com.kumpello.whereiseveryone.authentication.common.ui.TextField
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.AsyncState
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.entity.Logo
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainActivity

@Composable
fun LoginScreen(
    navController: NavController,
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
                is LoginViewModel.Action.MakeToast -> Toast.makeText(context, action.string, Toast.LENGTH_SHORT)
                    .show()

                LoginViewModel.Action.NavigateMain -> context.startActivity(Intent(context, MainActivity::class.java))
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
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 4.dp)
    ) {
        Logo.Image(
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField.Regular(
                label = "Username",
                value = viewState.username,
                onValueChange = { value ->
                    trigger(LoginViewModel.Event.SetUsername(value))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField.Password(
                label = "Password",
                value = viewState.password,
                onValueChange = { value ->
                    trigger(LoginViewModel.Event.SetPassword(value))
                },
                passwordVisible = viewState.passwordVisible,
                onTogglePasswordVisibility = {
                    trigger(LoginViewModel.Event.TogglePasswordVisibility)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button.Animated(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                enabled = !viewState.loginState.isLoading,
                text = "Login",
                textSize = 26,
                height = 50,
            ) { trigger(LoginViewModel.Event.OnLoginClick) }

            Spacer(modifier = Modifier.height(20.dp))

            Button.Animated(
                modifier = Modifier
                    .padding(horizontal = 40.dp),
                text = "Sign up here",
            ) { trigger(LoginViewModel.Event.NavigateSignUp) }
        }
    }
}

@Preview(showBackground = true)
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
