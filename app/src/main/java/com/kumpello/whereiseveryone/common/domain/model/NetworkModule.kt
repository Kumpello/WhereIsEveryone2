package com.kumpello.whereiseveryone.common.domain.model

import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationService
import com.kumpello.whereiseveryone.main.map.domain.repository.PositionsService
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {

    single { AuthenticationService() } bind AuthenticationService::class
    single { PositionsService() } bind PositionsService::class
    single { AuthenticationService() }

}