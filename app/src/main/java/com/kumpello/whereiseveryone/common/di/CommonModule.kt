package com.kumpello.whereiseveryone.common.di

import com.kumpello.whereiseveryone.common.data.provider.DeviceIdProviderImpl
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.provider.DeviceIdProvider
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import com.kumpello.whereiseveryone.common.domain.usecase.LogoutUseCase
import com.kumpello.whereiseveryone.common.domain.usecase.RefreshTokenUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val commonModule = module {
    single { EncryptedDataStoreRepository(androidContext()) }
    single { PreferencesManager(get()) }
    single<DeviceIdProvider> { DeviceIdProviderImpl(androidContext()) }
    single { RefreshTokenUseCase(get(), get(), get()) }
    single { LogoutUseCase(get(), get()) }
}
