package com.kumpello.whereiseveryone.main.common

import kotlinx.serialization.Serializable

@Serializable
internal sealed class MainNavigation(val route: String) {
    @Serializable
    object Map : MainNavigation("map")
    @Serializable
    object Settings : MainNavigation("settings")
    @Serializable
    object Friends : MainNavigation("friends")
}