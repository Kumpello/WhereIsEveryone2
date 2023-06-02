package com.kumpello.poker.ui.navigation

sealed class MainRoutes(val route: String) {
    object News : MainRoutes("news")
    object Games : MainRoutes("games")
    object Organizations : MainRoutes("organizations")
}