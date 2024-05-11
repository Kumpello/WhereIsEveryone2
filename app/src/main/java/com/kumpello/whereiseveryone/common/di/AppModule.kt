package com.kumpello.whereiseveryone.common.di

import com.google.android.gms.location.FusedLocationProviderClient
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
import com.kumpello.whereiseveryone.main.common.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetFriendsPositionsUseCase
import com.kumpello.whereiseveryone.main.map.domain.usecase.SendLocationUseCase
import com.kumpello.whereiseveryone.main.map.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.map.presentation.PositionsService
import com.kumpello.whereiseveryone.main.map.presentation.PositionsServiceImpl
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

//TODO Split into modules based on activity
val appModule = module {
    viewModel { SplashViewModel(get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { SignUpViewModel(get(), get(), get()) }
    viewModel {settingsViewModel ->
        MapViewModel(
            locationService = get(),
            positionsService = get(),
            settingsViewModel = settingsViewModel.get()
        )
    }
    viewModel { locationServiceInterface ->
        SettingsViewModel(
            locationServiceInterface = locationServiceInterface.get(),
            wipeLocationUseCase = get()
    ) }
    viewModel { FriendsViewModel(
        get(),
        get()
    ) }
    single<LocationService> {
        LocationServiceImpl(
            //fusedLocationClient = LocationServices.getFusedLocationProviderClient(androidContext())
        )
    }
    single<FusedLocationProviderClient> {
        LocationServices.getFusedLocationProviderClient(androidContext())
    }
    single<PositionsService> {
        PositionsServiceImpl()
    }
    single { WipeLocationUseCase() }
    single { PositionsServiceImpl() }
    single { ValidateLoginInputUseCase() }
    single { ValidatePasswordUseCase() }
    single { GetCurrentAuthKeyUseCase(get(), get()) }
    single { SaveKeyUseCase(get(), get()) }
    single { GetKeyUseCase(get()) }
    single { LoginUseCase(get(), get()) }
    single { SignUpUseCase(get(), get()) }
    single { SendLocationUseCase(get(), get()) }
    single { GetFriendsPositionsUseCase(get(), get(), get()) }
    single { GetEncryptedPreferencesUseCase(androidContext()) }
    single { AddFriendUseCase() }
    single { RemoveFriendUseCase() }
}