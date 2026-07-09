package com.kumpello.whereiseveryone.common.di

import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentRefreshTokenUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.LogoutUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.RefreshTokenUseCase
import com.kumpello.whereiseveryone.common.domain.ucecase.SaveKeyUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val commonModule = module {
    single { EncryptedDataStoreRepository(androidContext()) }
    single { GetKeyUseCase(get()) }
    single { SaveKeyUseCase(get()) }
    single { GetCurrentAuthTokenUseCase(get()) }
    single { GetCurrentRefreshTokenUseCase(get()) }
    single { RefreshTokenUseCase(get(), get(), get()) }
    single { LogoutUseCase(get(), get()) }
}
