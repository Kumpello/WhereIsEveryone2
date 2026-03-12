package com.kumpello.whereiseveryone.authentication.common

import kotlinx.serialization.Serializable

@Serializable
internal sealed class AuthenticationNavigation(val route: String) {
    @Serializable
    object Splash : AuthenticationNavigation("splash")
    @Serializable
    object Login : AuthenticationNavigation("login")
    @Serializable
    object SignUp : AuthenticationNavigation("signup")
}