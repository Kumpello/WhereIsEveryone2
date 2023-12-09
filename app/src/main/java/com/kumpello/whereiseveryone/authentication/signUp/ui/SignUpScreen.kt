package com.kumpello.whereiseveryone.authentication.signUp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumpello.whereiseveryone.common.entities.ScreenState
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.authentication.signUp.presentation.SignUpViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun SignUpScreen(
    navigator: DestinationsNavigator,
    viewModel: SignUpViewModel,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                is SignUpViewModel.Action.MakeToast -> TODO()
                SignUpViewModel.Action.NavigateMain -> TODO()
                SignUpViewModel.Action.NavigateLogin -> TODO()
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
    val mContext = LocalContext.current

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(text = "Sign up", style = TextStyle(fontSize = 40.sp, fontFamily = FontFamily.Default))

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Username") },
            value = viewState.username,
            onValueChange = { username ->
                trigger(SignUpViewModel.Command.SetUsername(username))
            })

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Password") },
            value = viewState.password,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = { password ->
                trigger(SignUpViewModel.Command.SetPassword(password))
            })

        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
            Button(
                onClick = {
                    trigger(SignUpViewModel.Command.SignUp)
                },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Sign up")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            ClickableText(
                text = AnnotatedString("Login here"),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                onClick = { trigger(SignUpViewModel.Command.NavigateLogin) },
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Default,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpPreview() {
    WhereIsEveryoneTheme {
        SignUpScreen(
            SignUpViewModel.ViewState(
                screenState = ScreenState.Success,
                username = "Janusz",
                password = "dupadupadupa"
            )
        ) {}
    }
}