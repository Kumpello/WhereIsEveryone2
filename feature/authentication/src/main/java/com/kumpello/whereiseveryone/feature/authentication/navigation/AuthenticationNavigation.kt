package com.kumpello.whereiseveryone.feature.authentication.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kumpello.whereiseveryone.authentication.common.AuthenticationRoute")
sealed interface AuthenticationRoute {
    @Serializable
    @SerialName("com.kumpello.whereiseveryone.authentication.common.AuthenticationRoute.Splash")
    data object Splash : AuthenticationRoute
    @Serializable
    @SerialName("com.kumpello.whereiseveryone.authentication.common.AuthenticationRoute.Login")
    data object Login : AuthenticationRoute
    @Serializable
    @SerialName("com.kumpello.whereiseveryone.authentication.common.AuthenticationRoute.SignUp")
    data object SignUp : AuthenticationRoute
}
