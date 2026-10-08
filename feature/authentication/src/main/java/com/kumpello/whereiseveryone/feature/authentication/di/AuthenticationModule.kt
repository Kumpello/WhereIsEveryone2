package com.kumpello.whereiseveryone.feature.authentication.di

import com.kumpello.whereiseveryone.feature.authentication.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.feature.authentication.domain.usecase.LoginUseCase
import com.kumpello.whereiseveryone.feature.authentication.ui.login.LoginViewModel
import com.kumpello.whereiseveryone.feature.authentication.domain.usecase.SignUpUseCase
import com.kumpello.whereiseveryone.feature.authentication.domain.usecase.ValidatePasswordUseCase
import com.kumpello.whereiseveryone.feature.authentication.ui.signup.SignUpViewModel
import com.kumpello.whereiseveryone.feature.authentication.ui.splash.SplashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val authenticationModule = module {
    viewModel { SplashViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { SignUpViewModel(get(), get(), get(), get()) }
    single { ValidateLoginInputUseCase() }
    single { ValidatePasswordUseCase() }
    single { LoginUseCase(get(), get(), get()) }
    single { SignUpUseCase(get(), get(), get()) }
}
