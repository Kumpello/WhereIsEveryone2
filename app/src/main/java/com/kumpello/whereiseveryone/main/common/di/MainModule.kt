package com.kumpello.whereiseveryone.main.common.di

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetNeededPermissionsUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.CalculateBearingUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.CalculateDistanceUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAccuracyUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAltUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertLastUpdateUseCase
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
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentRefreshTokenUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.RefreshTokenUseCase
import com.kumpello.whereiseveryone.main.common.domain.manager.FriendsManager
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceInterface
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
        ).build()
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
            getPermissionsStatusUseCase = get()
        )
    }
    viewModel {
        MessageViewModel(
            saveKeyUseCase = get(),
            getKeyUseCase = get(),
            updateStatusUseCase = get(),
        )
    }
    viewModel { (locationServiceInterface: LocationServiceInterface) ->
        SettingsViewModel(
            locationServiceInterface = locationServiceInterface,
            wipeLocationUseCase = get(),
            saveKeyUseCase = get(),
            getKeyUseCase = get()
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
        get()
    ) }
    viewModel { AddFriendViewModel(get()) }
    viewModel { ShareProfileViewModel(get()) }
    single<LocationService> {
        LocationServiceImpl()
    }
    single<FusedLocationProviderClient> {
        LocationServices.getFusedLocationProviderClient(androidContext())
    }
    single { FriendsManager(get(), get()) }
    single { GetPermissionsStatusUseCase(get()) }
    single { GetNeededPermissionsUseCase() }
    single { RefreshTokenUseCase(get(), get(), get()) }
    single { WipeLocationUseCase(get(), get()) }
    single { GetCurrentAuthTokenUseCase(get()) }
    single { GetCurrentRefreshTokenUseCase(get()) }
    single { SaveKeyUseCase(get()) }
    single { GetKeyUseCase(get()) }
    single { SendLocationUseCase(get(), get()) }
    single { GetFriendsDataUseCase(get(), get()) }
    single { EncryptedDataStoreRepository(androidContext()) }
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
    single { MapLocationUseCase(get(), get(), get()) }
    single { MapFriendUseCase(get(), get(), get(), get(), get()) }
    single { CalculateBearingUseCase() }
    single { CalculateDistanceUseCase() }
}
