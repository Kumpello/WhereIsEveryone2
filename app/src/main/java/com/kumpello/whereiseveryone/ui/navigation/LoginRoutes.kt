package com.kumpello.whereiseveryone.ui.navigation

sealed class LoginRoutes(val route: String) {
    object Login : LoginRoutes("login")
    object SignUp : LoginRoutes("sign_up")
    object Splash : LoginRoutes("splash")
}
