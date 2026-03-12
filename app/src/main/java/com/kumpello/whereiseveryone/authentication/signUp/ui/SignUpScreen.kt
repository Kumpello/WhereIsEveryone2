package com.kumpello.whereiseveryone.authentication.signUp.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.authentication.common.AuthenticationNavigation
import com.kumpello.whereiseveryone.authentication.common.ui.entity.TextField
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.PasswordValidationState
import com.kumpello.whereiseveryone.authentication.signUp.presentation.SignUpViewModel
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.ui.entity.Button
import com.kumpello.whereiseveryone.common.ui.entity.Logo
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainActivity

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: SignUpViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                is SignUpViewModel.Action.MakeToast -> Toast.makeText(
                    context,
                    action.string,
                    Toast.LENGTH_SHORT
                )
                    .show()

                SignUpViewModel.Action.NavigateMain -> context.startActivity(
                    Intent(
                        context,
                        MainActivity::class.java
                    )
                )

                SignUpViewModel.Action.NavigateLogin -> navController.navigate(
                    AuthenticationNavigation.Login.route)
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
    trigger: (SignUpViewModel.Command) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Logo.Text(
            modifier = Modifier
                .padding(vertical = 30.dp),
            size = 35
        )
        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                text = "Sign up",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField.Regular(
                label = "Username",
                value = viewState.username,
                onValueChange = { value ->
                    trigger(SignUpViewModel.Command.SetUsername(value))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField.Password(
                label = "Password",
                value = viewState.password,
                onValueChange = { password ->
                    trigger(SignUpViewModel.Command.SetPassword(password))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Conditions(viewState)

            Spacer(modifier = Modifier.height(20.dp))

            Button.Animated(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                enabled = viewState.passwordState.successful,
                text = "Sign up",
                textSize = 26,
                height = 50,
            ) { trigger(SignUpViewModel.Command.OnSignUpClick) }

            Spacer(modifier = Modifier.height(20.dp))

            Button.Animated(
                modifier = Modifier
                    .padding(horizontal = 40.dp),
                text = "Login here",
            ) { trigger(SignUpViewModel.Command.NavigateLogin) }
        }
    }
}

@Composable
fun Conditions(viewState: SignUpViewModel.ViewState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ConditionRow(
            condition = "Minimum 6 characters",
            checked = viewState.passwordState.hasMinimum
        )
        ConditionRow(
            condition = "Minimum 1 special character",
            checked = viewState.passwordState.hasSpecialCharacter
        )
        ConditionRow(
            condition = "Minimum 1 capitalized letter",
            checked = viewState.passwordState.hasCapitalizedLetter
        )
        ConditionRow(
            condition = "No whitespaces",
            checked = viewState.passwordState.noWhitespaces
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpPreview() {
    WhereIsEveryoneTheme {
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
                    passwordState = PasswordValidationState()
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
                    passwordState = PasswordValidationState()
                )
            ) {}
        }
    }
}