package com.kumpello.whereiseveryone.main.common

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface MainRoute {
    @Serializable
    data object Map : MainRoute
    @Serializable
    data object Settings : MainRoute
    @Serializable
    data object Friends : MainRoute
}
