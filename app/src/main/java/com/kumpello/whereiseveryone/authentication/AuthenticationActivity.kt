package com.kumpello.whereiseveryone.authentication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kumpello.whereiseveryone.authentication.common.AuthenticationNavigation
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.authentication.login.ui.LoginScreen
import com.kumpello.whereiseveryone.authentication.signUp.presentation.SignUpViewModel
import com.kumpello.whereiseveryone.authentication.signUp.ui.SignUpScreen
import com.kumpello.whereiseveryone.authentication.splash.presentation.SplashViewModel
import com.kumpello.whereiseveryone.authentication.splash.ui.SplashScreen
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthenticationActivity : ComponentActivity(), CoroutineScope by MainScope() {

    private val loginViewModel: LoginViewModel by viewModel()
    private val signUpViewModel: SignUpViewModel by viewModel()
    private val splashViewModel: SplashViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                //Runs on every launch
            }
        }
        setContent {
            WhereIsEveryoneTheme {
                AuthenticationScreen()
            }
        }
    }

    @Composable
    private fun AuthenticationScreen() {
        val navController = rememberNavController()
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = AuthenticationNavigation.Splash.route
            ) {
                composable(AuthenticationNavigation.Splash.route) {
                    SplashScreen(navController = navController, viewModel = splashViewModel)
                }
                composable(AuthenticationNavigation.Login.route) {
                    LoginScreen(navController = navController, viewModel = loginViewModel)
                }
                composable(AuthenticationNavigation.SignUp.route) {
                    SignUpScreen(navController = navController, viewModel = signUpViewModel)
                }
            }
        }
    }
}