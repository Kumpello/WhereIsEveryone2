package com.kumpello.whereiseveryone.common.domain.model

import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepositoryImpl
import com.kumpello.whereiseveryone.main.map.domain.repository.LocationRepository
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetFriendsPositionsUseCase
import com.kumpello.whereiseveryone.main.map.domain.usecase.SendPositionUseCase
import com.kumpello.whereiseveryone.main.map.domain.repository.LocationRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {

    single { AuthenticationRepositoryImpl() } bind AuthenticationRepository::class
    single { LocationRepositoryImpl() } bind LocationRepository::class
    single { SendPositionUseCase(get(), get()) }
    single { GetFriendsPositionsUseCase(get(), get(), get()) }
}