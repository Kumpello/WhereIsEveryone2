package com.kumpello.whereiseveryone.authentication.common

internal sealed class AuthenticationNavigation(val route: String) {
    object Splash : AuthenticationNavigation("splash")
    object Login : AuthenticationNavigation("login")
    object SignUp : AuthenticationNavigation("signup")
}