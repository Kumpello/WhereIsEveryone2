package com.kumpello.whereiseveryone.authentication.splash.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kumpello.whereiseveryone.R
import com.kumpello.whereiseveryone.authentication.common.AuthenticationRoute
import com.kumpello.whereiseveryone.authentication.splash.presentation.SplashViewModel
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainActivity

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.trigger(SplashViewModel.Event.CheckUserStatus)
    }

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                SplashViewModel.Action.NavigateMain -> {
                    context.startActivity(
                        Intent(
                            context,
                            MainActivity::class.java
                        )
                    )
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
            contentDescription = "Splash screen",
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
