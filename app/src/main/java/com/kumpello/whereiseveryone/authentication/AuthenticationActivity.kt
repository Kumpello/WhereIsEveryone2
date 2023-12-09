package com.kumpello.whereiseveryone.authentication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kumpello.whereiseveryone.NavGraphs
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.destinations.LoginScreenDestination
import com.kumpello.whereiseveryone.destinations.SignUpScreenDestination
import com.kumpello.whereiseveryone.destinations.SplashScreenDestination
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.authentication.signUp.presentation.SignUpViewModel
import com.kumpello.whereiseveryone.authentication.splash.presentation.SplashViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.dependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class AuthenticationActivity : ComponentActivity(), CoroutineScope by MainScope() {

    private val loginViewModel : LoginViewModel by inject()
    private val signUpViewModel : SignUpViewModel by inject()
    private val splashViewModel : SplashViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                //Runs on every launch
            }
        }
        setContent {
            WhereIsEveryoneTheme() {
                AuthenticationScreen()
            }
        }
    }

    @Composable
    private fun AuthenticationScreen() {
        WhereIsEveryoneTheme {
            DestinationsNavHost(
                navGraph = NavGraphs.root,
                dependenciesContainerBuilder = {
                    dependency(LoginScreenDestination) { loginViewModel }
                    dependency(SignUpScreenDestination) { signUpViewModel }
                    dependency(SplashScreenDestination) { splashViewModel }
                }
            )
        }
    }
}