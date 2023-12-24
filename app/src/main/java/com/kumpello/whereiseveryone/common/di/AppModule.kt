package com.kumpello.whereiseveryone.common.di

import com.kumpello.whereiseveryone.authentication.login.domain.usecase.LoginUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetEncryptedPreferencesUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.SignUpUseCase
import com.kumpello.whereiseveryone.authentication.signUp.presentation.SignUpViewModel
import com.kumpello.whereiseveryone.authentication.splash.presentation.SplashViewModel
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetFriendsPositionsUseCase
import com.kumpello.whereiseveryone.main.map.domain.usecase.SendPositionUseCase
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { SplashViewModel( get()) }
    single { LoginViewModel( get()) }
    single { SignUpViewModel( get()) }
    single { MapViewModel( get() ) }
    single { SettingsViewModel() }
    single { FriendsViewModel() }
    single { GetCurrentAuthKeyUseCase(get(), get()) }
    single { SaveKeyUseCase(get(), get()) }
    single { GetKeyUseCase(get()) }
    single { LoginUseCase(get(), get()) }
    single { SignUpUseCase(get(), get()) }
    single { SendPositionUseCase(get(), get()) }
    single { GetFriendsPositionsUseCase(get(), get(), get()) }
    single { GetEncryptedPreferencesUseCase(androidContext()) }
}