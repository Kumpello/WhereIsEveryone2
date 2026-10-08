package com.kumpello.whereiseveryone.feature.authentication.ui.splash

import com.kumpello.whereiseveryone.feature.authentication.R
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.feature.authentication.navigation.AuthenticationRoute
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme

@Composable
fun SplashScreen(
    navController: NavController,
    initialUri: Uri?,
    onAuthenticated: (Uri?) -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.trigger(SplashViewModel.Event.CheckUserStatus(initialUri))
    }

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                is SplashViewModel.Action.NavigateMain -> {
                    onAuthenticated(action.uri)
                }

                SplashViewModel.Action.NavigateSignUp -> {
                    navController.navigate(
                        AuthenticationRoute.SignUp
                    ) {
                        popUpTo(AuthenticationRoute.Splash) { inclusive = true }
                    }
                }

                SplashViewModel.Action.NavigateLogin -> {
                    navController.navigate(
                        AuthenticationRoute.Login
                    ) {
                        popUpTo(AuthenticationRoute.Splash) { inclusive = true }
                    }
                }
            }
        }
    }

    SplashScreen(
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black
    ) {
        Image(
            contentScale = ContentScale.FillHeight,
            painter = painterResource(id = R.drawable.im_splash_screen),
            contentDescription = stringResource(R.string.splash_screen_cd),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    WhereIsEveryoneTheme {
        SplashScreen(
            modifier = Modifier.fillMaxSize()
        )
    }
}
