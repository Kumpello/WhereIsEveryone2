package com.kumpello.whereiseveryone.main.common.di

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetNeededPermissionsUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAccuracyUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertAltUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.ConvertLastUpdateUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.SendLocationUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.WipeLocationUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AcceptFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RejectFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
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
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
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
            getKeyUseCase = get(),
            saveKeyUseCase = get(),
            updateStatusUseCase = get(),
            getPermissionsStatusUseCase = get(),
            convertAccuracyUseCase = get(),
            convertAltUseCase = get(),
            convertLastUpdateUseCase = get()
        )
    }
    viewModel { (locationServiceInterface: LocationServiceInterface) ->
        SettingsViewModel(
            locationServiceInterface = locationServiceInterface,
            wipeLocationUseCase = get()
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
    single { WipeLocationUseCase() }
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
    single { UpdateStatusUseCase(get(), get()) }
    single { ConvertAccuracyUseCase() }
    single { ConvertAltUseCase() }
    single { ConvertLastUpdateUseCase() }
}