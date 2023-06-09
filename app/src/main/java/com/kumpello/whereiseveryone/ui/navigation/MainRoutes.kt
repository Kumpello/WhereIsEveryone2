package com.kumpello.whereiseveryone.ui.navigation

sealed class MainRoutes(val route: String) {
    object Map : MainRoutes("map")
    object Settings : MainRoutes("settings")
    object Friends : MainRoutes("friends")
}