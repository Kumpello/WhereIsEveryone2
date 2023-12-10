package com.kumpello.whereiseveryone.authentication

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.RootNavGraph

@RootNavGraph(start = true)
@NavGraph
annotation class AuthenticationNavGraph(
    val start: Boolean = true
)