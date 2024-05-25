package com.kumpello.whereiseveryone.common.domain.model

import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepositoryImpl
import com.kumpello.whereiseveryone.main.common.domain.repository.FriendsRepository
import com.kumpello.whereiseveryone.main.common.domain.repository.FriendsRepositoryImpl
import com.kumpello.whereiseveryone.main.friends.domain.repository.ObserveRepository
import com.kumpello.whereiseveryone.main.friends.domain.repository.ObserveRepositoryImpl
import com.kumpello.whereiseveryone.main.friends.domain.usecase.AddFriendUseCase
import com.kumpello.whereiseveryone.main.friends.domain.usecase.RemoveFriendUseCase
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepository
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepositoryImpl
import com.kumpello.whereiseveryone.main.map.domain.repository.StatusRepository
import com.kumpello.whereiseveryone.main.map.domain.repository.StatusRepositoryImpl
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.common.domain.usecase.SendLocationUseCase
import com.kumpello.whereiseveryone.main.map.domain.usecase.UpdateStatusUseCase
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {

    single { AuthenticationRepositoryImpl() } bind AuthenticationRepository::class
    single { LocationRepositoryImpl() } bind LocationRepository::class
    single { FriendsRepositoryImpl() } bind FriendsRepository::class
    single { ObserveRepositoryImpl() } bind ObserveRepository::class
    single { StatusRepositoryImpl() } bind StatusRepository::class
    single { UpdateStatusUseCase(get(), get()) }
    single { SendLocationUseCase(get(), get()) }
    single { AddFriendUseCase(get(), get()) }
    single { RemoveFriendUseCase(get(), get()) }
    single { GetFriendsDataUseCase(get(), get()) }
}