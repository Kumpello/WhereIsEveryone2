package com.kumpello.whereiseveryone.common.di

import com.kumpello.whereiseveryone.authentication.domain.LoginUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetEncryptedPreferencesUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { LoginViewModel( get()) }
    single { MapViewModel( get() ) }
    single { SaveKeyUseCase(get(), get()) }
    single { GetKeyUseCase(get()) }
    single { LoginUseCase(get(), get()) }
    single { GetEncryptedPreferencesUseCase(androidContext()) }
}