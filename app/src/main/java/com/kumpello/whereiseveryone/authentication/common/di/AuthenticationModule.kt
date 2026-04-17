package com.kumpello.whereiseveryone.authentication.common.di

import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.login.domain.usecase.LoginUseCase
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.SignUpUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.ValidatePasswordUseCase
import com.kumpello.whereiseveryone.authentication.signUp.presentation.SignUpViewModel
import com.kumpello.whereiseveryone.authentication.splash.presentation.SplashViewModel
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val authenticationModule = module {
    viewModel { SplashViewModel(get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { SignUpViewModel(get(), get(), get()) }
    single { ValidateLoginInputUseCase() }
    single { ValidatePasswordUseCase() }
    single { GetCurrentAuthKeyUseCase(get(), get()) }
    single { SaveKeyUseCase(get()) }
    single { GetKeyUseCase(get()) }
    single { LoginUseCase(get(), get()) }
    single { SignUpUseCase(get(), get()) }
    single { EncryptedDataStoreRepository(androidContext()) }
}