package com.kumpello.whereiseveryone.authentication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kumpello.whereiseveryone.feature.authentication.navigation.AuthenticationRoute
import com.kumpello.whereiseveryone.feature.authentication.ui.login.LoginViewModel
import com.kumpello.whereiseveryone.feature.authentication.ui.login.LoginScreen
import com.kumpello.whereiseveryone.feature.authentication.ui.signup.SignUpViewModel
import com.kumpello.whereiseveryone.feature.authentication.ui.signup.SignUpScreen
import com.kumpello.whereiseveryone.feature.authentication.ui.splash.SplashViewModel
import com.kumpello.whereiseveryone.feature.authentication.ui.splash.SplashScreen
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthenticationActivity : ComponentActivity(), CoroutineScope by MainScope() {

    private val loginViewModel: LoginViewModel by viewModel()
    private val signUpViewModel: SignUpViewModel by viewModel()
    private val splashViewModel: SplashViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
                startDestination = AuthenticationRoute.Splash
            ) {
                composable<AuthenticationRoute.Splash> {
                    SplashScreen(
                        navController = navController,
                        initialUri = intent.data,
                        onAuthenticated = ::navigateMain,
                        viewModel = splashViewModel
                    )
                }
                composable<AuthenticationRoute.Login> {
                    LoginScreen(
                        navController = navController,
                        onAuthenticated = { navigateMain() },
                        viewModel = loginViewModel
                    )
                }
                composable<AuthenticationRoute.SignUp> {
                    SignUpScreen(
                        navController = navController,
                        onAuthenticated = { navigateMain() },
                        viewModel = signUpViewModel
                    )
                }
            }
        }
    }

    private fun navigateMain(uri: Uri? = null) {
        startActivity(Intent(this, MainActivity::class.java).apply { data = uri })
        finish()
    }
}
