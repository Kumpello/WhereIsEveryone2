package com.kumpello.whereiseveryone.authentication.login.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumpello.whereiseveryone.authentication.AuthenticationNavGraph
import com.kumpello.whereiseveryone.authentication.common.ui.entity.TextField
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.common.entities.ScreenState
import com.kumpello.whereiseveryone.common.ui.entities.Button
import com.kumpello.whereiseveryone.common.ui.entities.Logo
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.destinations.SignUpScreenDestination
import com.kumpello.whereiseveryone.main.MainActivity
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@AuthenticationNavGraph(start = false)
@Destination
@Composable
fun LoginScreen(
    navigator: DestinationsNavigator,
    viewModel: LoginViewModel,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel.action) {
        viewModel.action.collect { action ->
            when (action) {
                is LoginViewModel.Action.MakeToast -> Toast.makeText(context, action.string, Toast.LENGTH_SHORT)
                    .show()

                LoginViewModel.Action.NavigateMain -> context.startActivity(Intent(context, MainActivity::class.java))
                LoginViewModel.Action.NavigateSignUp -> navigator.navigate(SignUpScreenDestination)
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
    trigger: (LoginViewModel.Command) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Logo.Text(
            modifier = Modifier
                .padding(top = 30.dp)
                .align(Alignment.TopCenter),
            size = 35
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(15.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                text = "Login",
                style = TextStyle(
                    fontSize = 40.sp,
                    fontFamily = FontFamily.Default
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField.Regular(
                label = "Username",
                value = viewState.username,
                onValueChange = { value ->
                    trigger(LoginViewModel.Command.SetUsername(value))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField.Password(
                label = "Password",
                value = viewState.password,
                onValueChange = { value ->
                    trigger(LoginViewModel.Command.SetPassword(value))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .padding(40.dp, 0.dp, 40.dp, 0.dp)
            ) {
                Button.Animated(
                    text = "Login",
                    onClick = { trigger(LoginViewModel.Command.Login) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(40.dp)
                    .width(150.dp)
            ) {
                Button.Animated(
                    text = "Sign up here",
                    fontSize = 15
                ) {
                    trigger(LoginViewModel.Command.NavigateSignUp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    WhereIsEveryoneTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LoginScreen(
                LoginViewModel.ViewState(
                    screenState = ScreenState.Success,
                    username = "Janusz",
                    password = "dupadupadupa"
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
                    screenState = ScreenState.Success,
                    username = "Janusz",
                    password = "dupadupadupa"
                )
            ) {}
        }
    }
}