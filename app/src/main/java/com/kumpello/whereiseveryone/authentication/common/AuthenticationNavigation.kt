package com.kumpello.whereiseveryone.authentication.common

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface AuthenticationRoute {
    @Serializable
    data object Splash : AuthenticationRoute
    @Serializable
    data object Login : AuthenticationRoute
    @Serializable
    data object SignUp : AuthenticationRoute
}
