package com.kumpello.whereiseveryone.common.domain.model

import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetFriendsPositionsUseCase
import com.kumpello.whereiseveryone.main.map.domain.usecase.SendPositionUseCase
import com.kumpello.whereiseveryone.main.map.domain.repository.LocationRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {

    single { AuthenticationRepository() } bind AuthenticationRepository::class
    single { LocationRepository() } bind LocationRepository::class
    single { AuthenticationRepository() }
    single { SendPositionUseCase(get(), get()) }
    single { GetFriendsPositionsUseCase(get(), get(), get()) }
}