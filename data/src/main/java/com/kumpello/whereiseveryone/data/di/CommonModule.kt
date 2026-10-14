package com.kumpello.whereiseveryone.data.di

import androidx.room.Room
import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.data.repository.UserLocationRepository
import com.kumpello.whereiseveryone.data.repository.preferences.RememberedCredentialsRepository
import com.kumpello.whereiseveryone.data.local.database.AppDatabase
import com.kumpello.whereiseveryone.data.device.DeviceIdProviderImpl
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.device.DeviceIdProvider
import com.kumpello.whereiseveryone.data.local.preferences.EncryptedDataStoreRepository
import com.kumpello.whereiseveryone.data.session.LogoutUseCase
import com.kumpello.whereiseveryone.data.session.RefreshTokenUseCase
import com.kumpello.whereiseveryone.data.restore.AndroidRestoreCredentialClient
import com.kumpello.whereiseveryone.data.restore.RestoreCredentialClient
import com.kumpello.whereiseveryone.data.restore.RestoreCredentialsManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val commonModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "where-is-everyone-db")
            .fallbackToDestructiveMigration(false)
            .build()
    }
    single { get<AppDatabase>().friendDao() }
    single { get<AppDatabase>().userLocationDao() }
    single { FriendsStateRepository(get(), get()) }
    single { UserLocationRepository(get()) }
    single { RememberedCredentialsRepository(get()) }
    single { EncryptedDataStoreRepository(androidContext()) }
    single { PreferencesManager(get()) }
    single<DeviceIdProvider> { DeviceIdProviderImpl(androidContext()) }
    single<RestoreCredentialClient> { AndroidRestoreCredentialClient(androidContext()) }
    single { RestoreCredentialsManager(get(), get(), get(), get(), androidContext().packageName) }
    single { RefreshTokenUseCase(get(), get(), get(), get()) }
    single { LogoutUseCase(get(), get(), get()) }
}
