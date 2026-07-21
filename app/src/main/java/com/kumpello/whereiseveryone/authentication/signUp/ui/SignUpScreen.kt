package com.kumpello.whereiseveryone.authentication.signUp.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.common.AuthenticationRoute
import com.kumpello.whereiseveryone.authentication.common.ui.TextField
import com.kumpello.whereiseveryone.authentication.signUp.domain.model.PasswordValidationState
import com.kumpello.whereiseveryone.authentication.signUp.presentation.SignUpViewModel
import com.kumpello.whereiseveryone.common.entity.ScreenState
import com.kumpello.whereiseveryone.common.presentation.AsyncState
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
                is SignUpViewModel.Action.MakeToast -> Toast.makeText(context, action.string, Toast.LENGTH_SHORT)
                    .show()

                SignUpViewModel.Action.NavigateMain -> context.startActivity(Intent(context, MainActivity::class.java))
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
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .safeDrawingPadding()
            .padding(4.dp)
            .padding(horizontal = 20.dp)
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
                text = stringResource(R.string.signup_title),
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField.Regular(
                label = stringResource(R.string.username_label),
                value = viewState.username,
                onValueChange = { value ->
                    trigger(SignUpViewModel.Event.SetUsername(value))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField.Password(
                label = stringResource(R.string.password_label),
                value = viewState.password,
                onValueChange = { password ->
                    trigger(SignUpViewModel.Event.SetPassword(password))
                },
                passwordVisible = viewState.passwordVisible,
                onTogglePasswordVisibility = {
                    trigger(SignUpViewModel.Event.TogglePasswordVisibility)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Conditions(viewState)

            Spacer(modifier = Modifier.height(20.dp))

            Button.Animated(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                enabled = viewState.passwordState.successful && !viewState.signUpState.isLoading,
                text = stringResource(R.string.signup_title),
                textSize = 26,
                height = 50,
            ) { trigger(SignUpViewModel.Event.OnSignUpClick) }

            Spacer(modifier = Modifier.height(20.dp))

            Button.Animated(
                modifier = Modifier
                    .padding(horizontal = 40.dp),
                text = stringResource(R.string.login_here),
            ) { trigger(SignUpViewModel.Event.NavigateLogin) }
        }
    }
}

@Composable
fun Conditions(viewState: SignUpViewModel.ViewState) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = if (checked) Icons.Default.Check else Icons.Default.Close,
            contentDescription = stringResource(R.string.condition_cd),
            tint = if (checked) Color.Green else Color.Red
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = condition,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true)
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
