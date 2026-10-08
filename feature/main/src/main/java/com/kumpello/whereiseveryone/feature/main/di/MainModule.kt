package com.kumpello.whereiseveryone.feature.main.di

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kumpello.whereiseveryone.feature.main.ui.permissions.GetNeededPermissionsUseCase
import com.kumpello.whereiseveryone.feature.main.domain.manager.ProximityManager
import com.kumpello.whereiseveryone.feature.main.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapFriendUseCase
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapLocationUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.SendLocationUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.AcceptFriendUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.GetPausedFriendsUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.RejectFriendUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.ResumeSharingUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.StopSharingUseCase
import com.kumpello.whereiseveryone.feature.main.ui.friends.AddFriendViewModel
import com.kumpello.whereiseveryone.feature.main.ui.friends.FriendsViewModel
import com.kumpello.whereiseveryone.feature.main.ui.friends.ShareProfileViewModel
import com.kumpello.whereiseveryone.feature.main.sharing.nfc.NfcSharingSession
import com.kumpello.whereiseveryone.feature.main.ui.permissions.GetPermissionsStatusUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.UpdateStatusUseCase
import com.kumpello.whereiseveryone.feature.main.location.LocationService
import com.kumpello.whereiseveryone.feature.main.location.LocationServiceProxy
import com.kumpello.whereiseveryone.feature.main.ui.map.MapScreenViewModel
import com.kumpello.whereiseveryone.feature.main.ui.map.MapViewModel
import com.kumpello.whereiseveryone.feature.main.ui.map.MessageViewModel
import com.kumpello.whereiseveryone.feature.main.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainModule = module {
    viewModel {
        MapViewModel(
            locationService = get(),
            friendsManager = get(),
            mapLocationUseCase = get(),
            mapFriendUseCase = get(),
            stopSharingUseCase = get(),
            resumeSharingUseCase = get(),
            getPausedFriendsUseCase = get()
        )
    }
    viewModel {
        MapScreenViewModel(
            getPermissionsStatusUseCase = get(),
            locationService = get()
        )
    }
    viewModel {
        MessageViewModel(
            preferencesManager = get(),
            updateStatusUseCase = get(),
        )
    }
    viewModel {
        SettingsViewModel(
            locationService = get(),
            wipeLocationUseCase = get(),
            preferencesManager = get(),
            logoutUseCase = get()
        )
    }
    viewModel {
        FriendsViewModel(
            removeFriendUseCase = get(),
            getFriendsDataUseCase = get(),
            acceptFriendUseCase = get(),
            rejectFriendUseCase = get(),
            locationService = get(),
            mapFriendUseCase = get(),
            stopSharingUseCase = get(),
            resumeSharingUseCase = get(),
            getPausedFriendsUseCase = get(),
            preferencesManager = get(),
            nfcSharingSession = get()
        )
    }
    viewModel { AddFriendViewModel(get()) }
    viewModel { ShareProfileViewModel(get()) }

    single { NfcSharingSession() }

    single { LocationServiceProxy() }
    single<LocationService> { get<LocationServiceProxy>() }
    single<FusedLocationProviderClient> {
        LocationServices.getFusedLocationProviderClient(androidContext())
    }

    single { ProximityManager(get(), get(), get()) }

    single { GetPermissionsStatusUseCase(get()) }
    single { GetNeededPermissionsUseCase() }

    single { WipeLocationUseCase(get()) }
    single { SendLocationUseCase(get()) }
    single { GetFriendsDataUseCase(get()) }

    single { AddFriendUseCase(get()) }
    single { RemoveFriendUseCase(get()) }
    single { AcceptFriendUseCase(get()) }
    single { RejectFriendUseCase(get()) }

    single { StopSharingUseCase(get()) }
    single { ResumeSharingUseCase(get()) }
    single { GetPausedFriendsUseCase(get()) }
    single { UpdateStatusUseCase(get()) }

    single { MapLocationUseCase() }
    single { MapFriendUseCase() }
}
