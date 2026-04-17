package com.kumpello.whereiseveryone.main.common.di

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthKeyUseCase
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import com.kumpello.whereiseveryone.common.domain.ucecase.GetFriendsUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetNeededPermissionsUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveFriendsUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.SendLocationUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetPermissionsStatusUseCase
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.map.presentation.PositionsService
import com.kumpello.whereiseveryone.main.map.presentation.PositionsServiceImpl
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainModule = module {
    viewModel {
        MapViewModel(
            locationService = get(),
            positionsService = get(),
            getKeyUseCase = get(),
            saveKeyUseCase = get(),
            updateStatusUseCase = get(),
            getPermissionsStatusUseCase = get()
        )
    }
    viewModel { locationServiceInterface ->
        SettingsViewModel(
            locationServiceInterface = locationServiceInterface.get(),
            wipeLocationUseCase = get()
    ) }
    viewModel { FriendsViewModel(
        get(),
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
    single { GetPermissionsStatusUseCase(get()) }
    single { GetNeededPermissionsUseCase() }
    single { WipeLocationUseCase() }
    single { PositionsServiceImpl() }
    single { GetCurrentAuthKeyUseCase(get(), get()) }
    single { SaveKeyUseCase(get()) }
    single { GetKeyUseCase(get()) }
    single { GetFriendsUseCase(get()) }
    single { SaveFriendsUseCase(get(), get()) }
    single { SendLocationUseCase(get(), get()) }
    single { GetFriendsDataUseCase(get(), get()) }
    single { EncryptedDataStoreRepository(androidContext()) }
    single { AddFriendUseCase(get(), get()) }
    single { RemoveFriendUseCase(get(), get()) }
}