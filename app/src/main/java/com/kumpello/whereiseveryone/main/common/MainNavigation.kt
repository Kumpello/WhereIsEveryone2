package com.kumpello.whereiseveryone.main.common

internal sealed class MainNavigation(val route: String) {
    object Map : MainNavigation("map")
    object Settings : MainNavigation("settings")
    object Friends : MainNavigation("friends")
}