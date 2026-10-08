package com.kumpello.whereiseveryone.feature.main.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kumpello.whereiseveryone.main.common.MainRoute")
sealed interface MainRoute {
    @Serializable
    @SerialName("com.kumpello.whereiseveryone.main.common.MainRoute.Map")
    data object Map : MainRoute
    @Serializable
    @SerialName("com.kumpello.whereiseveryone.main.common.MainRoute.Settings")
    data object Settings : MainRoute
    @Serializable
    @SerialName("com.kumpello.whereiseveryone.main.common.MainRoute.Friends")
    data object Friends : MainRoute
}
