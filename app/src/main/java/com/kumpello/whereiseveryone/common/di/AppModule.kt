package com.kumpello.whereiseveryone.common.di

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.kumpello.whereiseveryone.authentication.common.domain.usecase.ValidateLoginInputUseCase
import com.kumpello.whereiseveryone.authentication.login.domain.usecase.LoginUseCase
import com.kumpello.whereiseveryone.authentication.login.presentation.LoginViewModel
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.SignUpUseCase
import com.kumpello.whereiseveryone.authentication.signUp.domain.usecase.ValidatePasswordUseCase
import com.kumpello.whereiseveryone.authentication.signUp.presentation.SignUpViewModel
import com.kumpello.whereiseveryone.authentication.splash.presentation.SplashViewModel
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetEncryptedPreferencesUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.PositionsServiceImpl
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetFriendsPositionsUseCase
import com.kumpello.whereiseveryone.main.map.domain.usecase.SendPositionUseCase
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.map.presentation.PositionsService
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

//TODO Split into modules based on activity
val appModule = module {
    single { SplashViewModel(get()) }
    single { LoginViewModel(get(), get()) }
    single { SignUpViewModel(get(), get(), get()) }
    single { ValidateLoginInputUseCase() }
    single { ValidatePasswordUseCase() }
    factory { (activityContext: Context) ->
        MapViewModel(
            LocationServiceImpl(LocationServices.getFusedLocationProviderClient(activityContext)),
            get()
        )
    }
    factory<LocationService> { (activityContext: Context) ->
        LocationServiceImpl(LocationServices.getFusedLocationProviderClient(activityContext))
    }
    factory<PositionsService> {
        PositionsServiceImpl()
    }
    single { PositionsServiceImpl() }
    //single { MapViewModel(get(), get()) } //TODO: Do something with services
    single { SettingsViewModel() }
    single { FriendsViewModel(get(), get()) }
    single { GetCurrentAuthKeyUseCase(get(), get()) }
    single { SaveKeyUseCase(get(), get()) }
    single { GetKeyUseCase(get()) }
    single { LoginUseCase(get(), get()) }
    single { SignUpUseCase(get(), get()) }
    single { SendPositionUseCase(get(), get()) }
    single { GetFriendsPositionsUseCase(get(), get(), get()) }
    single { GetEncryptedPreferencesUseCase(androidContext()) }
}