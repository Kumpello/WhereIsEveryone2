package com.kumpello.whereiseveryone.main.common.di

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kumpello.whereiseveryone.common.domain.usecase.GetNeededPermissionsUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.CalculateBearingUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.CalculateDistanceUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAccuracyUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAltUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertLastUpdateUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.FormatDateUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.FormatLastUpdateUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapFriendUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.MapLocationUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.SendLocationUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AcceptFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.GetPausedFriendsUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RejectFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.ResumeSharingUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.StopSharingUseCase
import com.kumpello.whereiseveryone.main.friends.presentation.AddFriendViewModel
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.friends.presentation.ShareProfileViewModel
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetPermissionsStatusUseCase
import com.kumpello.whereiseveryone.main.map.domain.usecase.UpdateStatusUseCase
import androidx.room.Room
import com.kumpello.whereiseveryone.common.database.AppDatabase
import com.kumpello.whereiseveryone.main.common.domain.manager.FriendsManager
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceProxy
import com.kumpello.whereiseveryone.main.map.presentation.MapScreenViewModel
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.map.presentation.MessageViewModel
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "where-is-everyone-db"
        ).fallbackToDestructiveMigration().build()
    }
    single { get<AppDatabase>().friendDao() }
    single { get<AppDatabase>().userLocationDao() }

    viewModel {
        MapViewModel(
            locationService = get(),
            friendsManager = get(),
            mapLocationUseCase = get(),
            mapFriendUseCase = get(),
            calculateBearingUseCase = get(),
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
    viewModel { FriendsViewModel(
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get()
    ) }
    viewModel { AddFriendViewModel(get()) }
    viewModel { ShareProfileViewModel(get()) }
    single { LocationServiceProxy() }
    single<LocationService> { get<LocationServiceProxy>() }
    single<FusedLocationProviderClient> {
        LocationServices.getFusedLocationProviderClient(androidContext())
    }
    single { FriendsManager(get(), get()) }
    single { GetPermissionsStatusUseCase(get()) }
    single { GetNeededPermissionsUseCase() }
    single { WipeLocationUseCase(get(), get()) }
    single { SendLocationUseCase(get(), get()) }
    single { GetFriendsDataUseCase(get(), get()) }
    single { AddFriendUseCase(get(), get()) }
    single { RemoveFriendUseCase(get(), get()) }
    single { AcceptFriendUseCase(get(), get()) }
    single { RejectFriendUseCase(get(), get()) }
    single { StopSharingUseCase(get(), get()) }
    single { ResumeSharingUseCase(get(), get()) }
    single { GetPausedFriendsUseCase(get(), get()) }
    single { UpdateStatusUseCase(get(), get()) }
    single { ConvertAccuracyUseCase() }
    single { ConvertAltUseCase() }
    single { ConvertLastUpdateUseCase() }
    single { FormatLastUpdateUseCase() }
    single { FormatDateUseCase() }
    single { MapLocationUseCase(get(), get(), get()) }
    single { MapFriendUseCase(get(), get(), get(), get(), get(), get()) }
    single { CalculateBearingUseCase() }
    single { CalculateDistanceUseCase() }
}
