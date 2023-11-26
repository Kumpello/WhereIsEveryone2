package com.kumpello.whereiseveryone.common.di

import com.kumpello.whereiseveryone.login.presentation.LoginActivityViewModel

val viewModelsModule = module {
    single<LoginActivityViewModel> { LoginActivityViewModel() }
}